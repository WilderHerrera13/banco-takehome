package com.bp.account.models;

import java.math.BigDecimal;

public enum MovementType {

    DEPOSIT(1),
    WITHDRAWAL(-1);

    private final int signo;

    MovementType(int signo) {
        this.signo = signo;
    }

    public int getSigno() {
        return signo;
    }

    public BigDecimal apply(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(signo));
    }
}
