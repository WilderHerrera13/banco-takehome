package com.bp.shared.dto;

import java.math.BigDecimal;

public record AccountDto(
        String accountNumber,
        String accountType,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        Boolean active,
        String clientId) {
}
