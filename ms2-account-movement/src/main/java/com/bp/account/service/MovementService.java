package com.bp.account.service;

import com.bp.account.models.Account;
import com.bp.account.models.Movement;
import com.bp.account.models.MovementType;
import com.bp.account.exception.AccountNotFoundException;
import com.bp.account.repository.AccountRepository;
import com.bp.account.mappers.MovementMapper;
import com.bp.account.repository.MovementRepository;
import com.bp.shared.dto.MovementDto;
import com.bp.shared.event.MovementRegisteredEvent;
import com.bp.shared.exception.InsufficientFundsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class MovementService {

    private static final Logger LOG = LoggerFactory.getLogger(MovementService.class);

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final MovementEventPublisher eventPublisher;
    private final MovementMapper movementMapper;

    public MovementService(AccountRepository accountRepository, MovementRepository movementRepository,
            MovementEventPublisher eventPublisher, MovementMapper movementMapper) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.movementMapper = movementMapper;
    }

    @Transactional
    public MovementDto register(String accountNumber, LocalDate date, MovementType movementType,
            BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        BigDecimal valorConSigno = movementType.apply(amount);
        BigDecimal resultingBalance = account.getCurrentBalance().add(valorConSigno);

        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            LOG.warn("Movement rejected on account {}: balance {} amount {}", accountNumber,
                    account.getCurrentBalance(), valorConSigno);
            throw new InsufficientFundsException(accountNumber, account.getCurrentBalance(), valorConSigno);
        }

        Movement movement = movementRepository
                .save(new Movement(date, movementType, valorConSigno, resultingBalance, accountNumber));
        account.setCurrentBalance(resultingBalance);
        accountRepository.save(account);

        eventPublisher.publish(new MovementRegisteredEvent(movement.getId(), account.getClientId(),
                accountNumber, movementType.name(), valorConSigno, resultingBalance, date, Instant.now()));

        LOG.info("Movement {} registered on account {} for {}", movement.getId(), accountNumber, valorConSigno);
        return toDto(movement);
    }

    @Transactional(readOnly = true)
    public List<MovementDto> listByAccount(String accountNumber) {
        return movementRepository.findByAccountNumberOrderByDateAsc(accountNumber).stream()
                .map(movementMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementDto> listAll() {
        return movementRepository.findAll().stream().map(movementMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MovementDto get(Long id) {
        return movementRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Movement no encontrado: " + id));
    }

    @Transactional
    public MovementDto update(Long id, LocalDate date, MovementType movementType, BigDecimal amount) {
        Movement movement = movementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movement no encontrado: " + id));
        Account account = accountRepository.findByAccountNumberForUpdate(movement.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(movement.getAccountNumber()));
        movement.setDate(date);
        movement.setMovementType(movementType);
        movement.setAmount(movementType.apply(amount));
        movementRepository.save(movement);
        recalculateBalance(account);
        return toDto(movement);
    }

    private void recalculateBalance(Account account) {
        BigDecimal balance = account.getInitialBalance();
        for (Movement movement : movementRepository.findByAccountNumberOrderByDateAsc(account.getAccountNumber())) {
            balance = balance.add(movement.getAmount());
            movement.setBalance(balance);
            movementRepository.save(movement);
        }
        account.setCurrentBalance(balance);
        accountRepository.save(account);
    }

    private MovementDto toDto(Movement movement) {
        return new MovementDto(movement.getId(), movement.getDate(), movement.getMovementType().name(),
                movement.getAmount(), movement.getBalance(), movement.getAccountNumber());
    }
}
