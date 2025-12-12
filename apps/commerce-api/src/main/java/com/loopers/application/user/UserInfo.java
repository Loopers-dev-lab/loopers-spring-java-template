package com.loopers.application.user;


import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

public record UserInfo(
        String loginId,
        String email,
        String birthDate,
        Gender gender
) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getLoginIdValue(),
                user.getEmailValue(),
                user.getBirthDateValue(),
                user.getGender()
        );
    }
}
