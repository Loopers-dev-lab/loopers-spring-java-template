package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;

public record UserInfo(Long id, String userId, String email, String birthDate, Gender gender) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
                model.getId(),
                model.getUserId(),
                model.getEmail(),
                model.getBirthDate(),
                model.getGender()
        );
    }
}
