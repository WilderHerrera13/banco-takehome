package com.bp.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementDto(
        Long id,
        LocalDate date,
        String movementType,
        BigDecimal amount,
        BigDecimal balance,
        String accountNumber) {
}
