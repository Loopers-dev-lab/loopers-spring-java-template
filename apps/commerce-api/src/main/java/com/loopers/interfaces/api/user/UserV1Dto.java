package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1Dto {
    public record UserResponse(String userId, String email, String birthDate) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.userId(),
                    info.email(),
                    info.birthDate()
            );
        }
    }
}
