package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.time.LocalDate;

public record UserInfo(Long id, String loginId, String email, LocalDate birthDate, Gender gender) {

    public static UserInfo from(User user) {
        return new UserInfo(
            user.getId(),
            user.getLoginId(),
            user.getEmail(),
            user.getBirthDate(),
            user.getGender()
        );
    }
}
