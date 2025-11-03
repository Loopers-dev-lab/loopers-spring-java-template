package com.loopers.interfaces.api.user;

public record UserResponseDto(
        String id,
        String email,
        String birthDate,
        String gender
) {}
