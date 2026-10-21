package com.bp.shared.dto;

public record ClientDto(
        String clientId,
        String name,
        String gender,
        Integer age,
        String identification,
        String address,
        String phone,
        String password,
        Boolean active) {
}
