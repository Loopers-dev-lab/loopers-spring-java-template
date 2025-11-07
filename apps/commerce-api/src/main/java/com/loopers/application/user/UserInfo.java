package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(Long id, String loginId, String email, String birthDate, String gender) {

    public static UserInfo from(User user) {
        return new UserInfo(
            user.getId(),
            user.getLoginId(),
            user.getEmail(),
            user.getBirthDate(),
            user.getGender()
        );
    }
}
