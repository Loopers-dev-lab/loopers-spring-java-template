package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(
        String id,
        String email,
        String birthDate,
        String gender
) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
        );
    }
}
