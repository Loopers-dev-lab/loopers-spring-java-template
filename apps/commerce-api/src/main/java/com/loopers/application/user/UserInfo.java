package com.loopers.application.user;


import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

public record UserInfo(
        String userId,
        String email,
        String birthDate,
        Gender gender
) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getUserIdValue(),
                user.getEmailValue(),
                user.getBirthDateValue(),
                user.getGender()
        );
    }
}
