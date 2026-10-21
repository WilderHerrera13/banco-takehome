package com.bp.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountStatementDto(
        String clientId,
        String client,
        LocalDate from,
        LocalDate to,
        List<AccountReport> accounts,
        List<MovementReport> movements) {

    public record AccountReport(
            String accountNumber,
            String accountType,
            BigDecimal initialBalance,
            BigDecimal currentBalance,
            Boolean active) {
    }

    public record MovementReport(
            LocalDate date,
            String accountNumber,
            String movementType,
            BigDecimal amount,
            BigDecimal balance) {
    }
}
