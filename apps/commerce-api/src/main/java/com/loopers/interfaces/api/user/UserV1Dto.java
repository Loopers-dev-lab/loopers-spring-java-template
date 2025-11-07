package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.User;

public class UserV1Dto {
    public record SignupRequest(String loginId, String password, String email, String birthDate, String gender) {
        public User toEntity() {
            return User.create(loginId, password, email, birthDate, gender);
        }
    }

    public record UserResponse(Long id, String loginId, String email, String birthDate, String gender) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.id(),
                    info.loginId(),
                    info.email(),
                    info.birthDate(),
                    info.gender()
            );
        }
    }

}
