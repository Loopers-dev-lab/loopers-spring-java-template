package com.loopers.interfaces.api.user;

public record UserRequestDto(
        String id,
        String email,
        String birthDate,
        String gender
) {}

