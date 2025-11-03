package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(
        String userId,
        String userName,
        String description,
        String email,
        String birthDate,
        String gender,
        Integer point) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getUserId(),
            model.getUserName(),
            model.getDescription(),
            model.getEmail(),
            model.getBirthDate(),
            model.getGender(),
            model.getPoint()
        );
    }
}
