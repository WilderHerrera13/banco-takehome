package com.bp.shared.dto;

public record PersonDto(
        String name,
        String gender,
        Integer age,
        String identification,
        String address,
        String phone) {
}
