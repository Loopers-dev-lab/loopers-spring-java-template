package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import java.time.LocalDate;

public record UserInfo(String id, String email, LocalDate birthDate, String gender, Integer point) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getUserId(),
            model.getEmail(),
            model.getBirthDate(),
            model.getGender(),
            model.getPoint()
        );
    }
}
