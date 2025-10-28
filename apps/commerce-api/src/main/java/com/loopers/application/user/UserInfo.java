package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(Long id, String userId, String email, String birthdate, String gender) {
    public static UserInfo from(UserModel userModel) {
        return new UserInfo(
                userModel.getId(),
                userModel.getUserId(),
                userModel.getEmail(),
                userModel.getBirthdate(),
                userModel.getGender()
        );
    }
}
