package com.bp.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MovementRegisteredEvent(
        Long movementId,
        String clientId,
        String accountNumber,
        String movementType,
        BigDecimal amount,
        BigDecimal balance,
        LocalDate date,
        Instant fechaEvento) {
}
