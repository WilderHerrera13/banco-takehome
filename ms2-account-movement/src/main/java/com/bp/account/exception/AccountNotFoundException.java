package com.bp.account.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account no encontrada: " + accountNumber);
    }
}
