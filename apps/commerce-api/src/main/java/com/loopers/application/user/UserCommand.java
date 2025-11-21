package com.loopers.application.user;

import com.loopers.domain.user.Gender;

public record UserCommand(
        String userId,
        String email,
        String birthDate,
        Gender gender
) {
}
