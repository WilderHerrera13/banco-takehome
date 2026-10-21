package com.bp.client.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ClientRequest(
        @NotBlank String name,
        @Pattern(regexp = "(?i)(MALE|FEMALE|OTHER)", message = "must be MALE, FEMALE or OTHER")
        String gender,
        @PositiveOrZero Integer age,
        @NotBlank String identification,
        String address,
        String phone,
        @NotBlank String password,
        Boolean active) {
}
