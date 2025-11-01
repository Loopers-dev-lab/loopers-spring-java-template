package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;

public class UserV1Dto {
    public record UserResponse(Long id, String loginId, String email, Gender gender, String birth) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.id(),
                    userInfo.loginId(),
                    userInfo.email(),
                    userInfo.gender(),
                    userInfo.birth()
            );
        }
    }

    public record SignUpRequest(String loginId, String email, Gender gender, String birth, String password) {
        public UserEntity toEntity() {
            return new UserEntity(
                loginId,
                email,
                gender,
                birth,
                password
            );
        }
    }

}
