package com.bp.client.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ClientRequest(
        @NotBlank String name,
        String gender,
        @PositiveOrZero Integer age,
        @NotBlank String identification,
        String address,
        String phone,
        @NotBlank String password,
        Boolean active) {
}
