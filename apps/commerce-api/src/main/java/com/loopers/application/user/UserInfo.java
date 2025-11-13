package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.math.BigDecimal;

public record UserInfo(Long id, String userId, String email, String birthdate, Gender gender, BigDecimal point) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getUserId(),
                user.getEmail(),
                user.getBirthdate(),
                user.getGender(),
                user.getPoint().getAmount()
        );
    }
}
