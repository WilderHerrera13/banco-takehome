package com.bp.account.controllers;

import com.bp.account.service.AccountService;
import com.bp.account.controllers.dto.AccountRequest;
import com.bp.shared.dto.AccountDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDto> create(@Valid @RequestBody AccountRequest request) {
        AccountDto account = accountService.create(new AccountDto(request.accountNumber(), request.accountType(),
                request.initialBalance(), request.initialBalance(), request.active(), request.clientId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping
    public List<AccountDto> list(@RequestParam(required = false) String clientId) {
        return clientId == null || clientId.isBlank() ? accountService.list() : accountService.listByClient(clientId);
    }

    @GetMapping("/{accountNumber}")
    public AccountDto get(@PathVariable String accountNumber) {
        return accountService.get(accountNumber);
    }

    @PutMapping("/{accountNumber}")
    public AccountDto update(@PathVariable String accountNumber, @Valid @RequestBody AccountRequest request) {
        return accountService.update(accountNumber, new AccountDto(accountNumber, request.accountType(),
                request.initialBalance(), request.initialBalance(), request.active(), request.clientId()));
    }
}
