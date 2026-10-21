package com.bp.account;

import com.bp.account.models.Account;
import com.bp.account.models.AccountType;
import com.bp.account.models.Movement;
import com.bp.account.models.MovementType;
import com.bp.account.exception.AccountNotFoundException;
import com.bp.account.repository.AccountRepository;
import com.bp.account.repository.MovementRepository;
import com.bp.account.service.MovementEventPublisher;
import com.bp.account.service.MovementService;
import com.bp.shared.dto.MovementDto;
import com.bp.shared.event.MovementRegisteredEvent;
import com.bp.shared.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test of the movement registration (F2 and F3)")
class MovementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private MovementEventPublisher eventPublisher;

    @InjectMocks
    private MovementService movementService;

    @Test
    @DisplayName("a deposit updates the available balance and records the transaction")
    void shouldRegisterDeposit() {
        Account account = new Account("225487", AccountType.CURRENT, new BigDecimal("100.00"), true, "cli-1");
        when(accountRepository.findByAccountNumberForUpdate("225487")).thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovementDto result = movementService.register("225487", LocalDate.of(2022, 2, 10), MovementType.DEPOSIT,
                new BigDecimal("600"));

        assertThat(result.amount()).isEqualByComparingTo("600.00");
        assertThat(result.balance()).isEqualByComparingTo("700.00");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("700.00");

        ArgumentCaptor<MovementRegisteredEvent> event = ArgumentCaptor.forClass(MovementRegisteredEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().balance()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("an update that would leave the balance in negative is rejected")
    void shouldRejectUpdateThatBreaksTheBalance() {
        Account account = new Account("496825", AccountType.SAVINGS, new BigDecimal("0.00"), true, "cli-1");
        Movement movement = new Movement(LocalDate.of(2022, 2, 8), MovementType.WITHDRAWAL, new BigDecimal("-40.00"),
                new BigDecimal("0.00"), "496825");
        when(movementRepository.findById(20L)).thenReturn(Optional.of(movement));
        when(accountRepository.findByAccountNumberForUpdate("496825")).thenReturn(Optional.of(account));
        when(movementRepository.findByAccountNumberOrderByDateAsc("496825")).thenReturn(List.of(movement));
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> movementService.update(20L, LocalDate.of(2022, 2, 8), MovementType.WITHDRAWAL,
                new BigDecimal("500")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Saldo no disponible");

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("a withdrawal without available balance is rejected")
    void shouldRejectWithdrawalWithoutBalance() {
        Account account = new Account("496825", AccountType.SAVINGS, new BigDecimal("540.00"), true, "cli-1");
        when(accountRepository.findByAccountNumberForUpdate("496825")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> movementService.register("496825", LocalDate.of(2022, 2, 8),
                MovementType.WITHDRAWAL, new BigDecimal("540.01")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Saldo no disponible");

        verify(movementRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(MovementRegisteredEvent.class));
    }

    @Test
    @DisplayName("registering on an unknown account fails")
    void shouldRejectUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movementService.register("no-existe", LocalDate.of(2022, 2, 10),
                MovementType.DEPOSIT, new BigDecimal("10")))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
