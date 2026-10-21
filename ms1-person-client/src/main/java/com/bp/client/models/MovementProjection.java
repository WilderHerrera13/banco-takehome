package com.bp.client.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movement_projection")
public class MovementProjection {

    @Id
    @Column(name = "movement_id")
    private Long movementId;

    @Column(name = "client_id", nullable = false, length = 40)
    private String clientId;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    protected MovementProjection() {
    }

    public MovementProjection(Long movementId, String clientId, String accountNumber, BigDecimal amount,
            BigDecimal balance, LocalDate date) {
        this.movementId = movementId;
        this.clientId = clientId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balance = balance;
        this.date = date;
    }

    public Long getMovimientoId() {
        return movementId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDate getDate() {
        return date;
    }
}
