package com.bp.account.service;

import com.bp.account.models.Account;
import com.bp.account.models.AccountType;
import com.bp.account.exception.AccountNotFoundException;
import com.bp.account.mappers.AccountMapper;
import com.bp.account.repository.AccountRepository;
import com.bp.shared.dto.AccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountDto create(AccountDto request) {
        if (accountRepository.existsById(request.accountNumber())) {
            throw new IllegalStateException("La account ya existe: " + request.accountNumber());
        }
        Account account = accountMapper.toEntity(request);
        Account guardada = accountRepository.save(account);
        LOG.info("Account {} created for client {}", guardada.getAccountNumber(), guardada.getClientId());
        return accountMapper.toDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> listByClient(String clientId) {
        return accountRepository.findByClientIdAndActiveTrueOrderByAccountNumber(clientId).stream()
                .map(accountMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountDto> list() {
        return accountRepository.findAll().stream().map(accountMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AccountDto get(String accountNumber) {
        return accountMapper.toDto(accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber)));
    }

    @Transactional
    public AccountDto update(String accountNumber, AccountDto request) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        account.setAccountType(AccountType.valueOf(request.accountType()));
        account.setActive(request.active() == null || request.active());
        Account guardada = accountRepository.save(account);
        LOG.info("Account {} updated", accountNumber);
        return accountMapper.toDto(guardada);
    }

}
