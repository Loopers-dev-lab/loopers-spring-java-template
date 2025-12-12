package com.loopers.application.user;

import com.loopers.domain.user.Gender;

public record UserCommand(
        String loginId,
        String email,
        String birthDate,
        Gender gender
) {
}
