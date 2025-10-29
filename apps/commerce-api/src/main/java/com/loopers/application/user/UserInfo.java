package com.loopers.application.user;

import com.loopers.domain.user.User;

import java.time.LocalDate;

public record UserInfo(String userId, String email, LocalDate birth) {
    public static UserInfo from(User user) {
        return new UserInfo(
            user.getUserId(),
            user.getEmail(),
            user.getBirth()
        );
    }
}
