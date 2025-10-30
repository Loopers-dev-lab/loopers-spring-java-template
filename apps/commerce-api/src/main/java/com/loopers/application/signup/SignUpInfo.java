package com.loopers.application.signup;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.time.LocalDate;

public record SignUpInfo(Long id, String userId, String email, LocalDate birthDate, Gender gender) {
    public static SignUpInfo from(User user) {
        return new SignUpInfo(
                user.getId(),
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
        );
    }
}
