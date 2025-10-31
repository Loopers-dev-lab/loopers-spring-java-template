package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;

public record UserInfo(Long id, String userId, String email, String birthdate, Gender gender, Integer point) {
    public static UserInfo from(UserModel userModel) {
        return new UserInfo(
                userModel.getId(),
                userModel.getUserId(),
                userModel.getEmail(),
                userModel.getBirthdate(),
                userModel.getGender(),
                userModel.getPoint()
        );
    }
}
