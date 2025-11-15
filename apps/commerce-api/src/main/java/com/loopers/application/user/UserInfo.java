package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(String userId, String email, String birthDate, String gender) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getUserId().userId(),
            model.getEmail().email(),
            model.getBirthDate().birthDate(),
            model.getGender().gender()
        );
    }
}
