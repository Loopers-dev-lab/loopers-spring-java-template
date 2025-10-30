package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1DTO {

    public record UserRequest(String userId, String email, String birthdate, String gender) {

    }
    public record UserResponse(Long id, String userId, String email, String birthdate, String gender, Integer point) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.id(),
                    userInfo.userId(),
                    userInfo.email(),
                    userInfo.birthdate(),
                    userInfo.gender(),
                    userInfo.point()
            );
        }
    }

    public record UserPointResponse(String userId, Integer point) {
        public static UserPointResponse from(String userId, Integer point) {
            return new UserPointResponse(
                    userId,
                    point
            );
        }
    }
}
