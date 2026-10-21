package com.bp.account.service;

import com.bp.account.models.ClientProjection;
import com.bp.account.models.Account;
import com.bp.account.models.Movement;
import com.bp.account.exception.UnknownClientException;
import com.bp.account.repository.ClientProjectionRepository;
import com.bp.account.repository.AccountRepository;
import com.bp.account.repository.MovementRepository;
import com.bp.shared.dto.AccountStatementDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountStatementService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final ClientProjectionRepository clientProjectionRepository;

    public AccountStatementService(AccountRepository accountRepository, MovementRepository movementRepository,
            ClientProjectionRepository clientProjectionRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.clientProjectionRepository = clientProjectionRepository;
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generate(String clientId, LocalDate from, LocalDate to) {
        ClientProjection client = clientProjectionRepository.findByClientId(clientId)
                .orElseThrow(() -> new UnknownClientException(clientId));

        List<Account> accounts = accountRepository.findByClientIdAndActiveTrueOrderByAccountNumber(clientId);
        List<Movement> movements = movementRepository
                .findMovementsByClientAndPeriod(clientId, from, to);

        List<AccountStatementDto.AccountReport> accountReports = accounts.stream()
                .map(c -> new AccountStatementDto.AccountReport(c.getAccountNumber(), c.getAccountType().name(),
                        c.getInitialBalance(), c.getCurrentBalance(), c.isActive()))
                .toList();

        List<AccountStatementDto.MovementReport> movementReports = movements.stream()
                .map(m -> new AccountStatementDto.MovementReport(m.getDate(), m.getAccountNumber(),
                        m.getMovementType().name(), m.getAmount(), m.getBalance()))
                .toList();

        return new AccountStatementDto(clientId, client.getName(), from, to, accountReports, movementReports);
    }
}
