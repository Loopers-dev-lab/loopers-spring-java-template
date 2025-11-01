package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;

public class UserV1Dto {
    public record UserResponse(Long id, String loginId, Gender gender, String email, String birth) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.id(),
                    userInfo.loginId(),
                    userInfo.gender(),
                    userInfo.email(),
                    userInfo.birth()
            );
        }
    }

    public record SignUpRequest(String loginId, Gender gender, String email, String birth, String password) {
        public UserEntity toEntity() {
            return new UserEntity(
                loginId,
                gender,
                email,
                birth,
                password
            );
        }
    }

}
