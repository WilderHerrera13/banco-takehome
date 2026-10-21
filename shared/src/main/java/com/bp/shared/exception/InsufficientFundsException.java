package com.bp.shared.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private final String accountNumber;
    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String accountNumber, BigDecimal availableBalance, BigDecimal requestedAmount) {
        super("Saldo no disponible");
        this.accountNumber = accountNumber;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getSaldoDisponible() {
        return availableBalance;
    }

    public BigDecimal getValorSolicitado() {
        return requestedAmount;
    }
}
