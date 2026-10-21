package com.bp.account.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank String accountNumber,
        @NotBlank String accountType,
        @NotNull @PositiveOrZero BigDecimal initialBalance,
        Boolean active,
        @NotBlank String clientId) {
}
