package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1Dto {
    public record RegisterRequest(
            String userId,
            String mail,
            String birth,
            String gender
    ) {
    }

    public record UserResponse(String userId, String email, String birth, String gender) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.userId(),
                    info.email(),
                    info.birth(),
                    info.gender()
            );
        }
    }
}
