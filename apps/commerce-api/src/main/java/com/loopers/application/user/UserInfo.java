package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(String userId, String email, String birthDate) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getUserId(),
            model.getEmail(),
            model.getBirthDate()
        );
    }
}
