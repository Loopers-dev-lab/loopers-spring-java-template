package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1Dto {
    public record UserResponse(
            String id,
            String email,
            String birthday,
            String gender) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.id(),
                    info.email(),
                    info.birthday(),
                    info.gender()
            );
        }
    }

    public record UserRegisterRequest(
            String id,
            String email,
            String birthday,
            String gender) {
    }
}
