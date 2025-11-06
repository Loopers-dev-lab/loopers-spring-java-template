package com.loopers.application.user;

import java.time.LocalDate;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;

public record UserInfo(Long id,
                       String username,
                       String email,
                       LocalDate birthdate,
                       Gender gender) {
    public static UserInfo from(UserEntity entity) {
        return new UserInfo(entity.getId(), entity.getUsername(), entity.getEmail(), entity.getBirthdate(), entity.getGender());
    }

}
