package com.bp.account.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementRequest(
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "(?i)(DEPOSIT|WITHDRAWAL)", message = "must be DEPOSIT or WITHDRAWAL")
        String movementType,
        @NotNull @Positive BigDecimal amount) {
}
