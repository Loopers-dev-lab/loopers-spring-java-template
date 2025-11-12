package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.time.LocalDate;

public record UserResult(String loginId, String email, LocalDate birth, Gender gender) {
    public static UserResult from(User user) {
        return new UserResult(
            user.getLoginId(),
            user.getEmail(),
            user.getBirth(),
            user.getGender()
        );
    }
}
