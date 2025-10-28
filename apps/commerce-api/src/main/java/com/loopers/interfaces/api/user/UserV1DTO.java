package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserModel;

public class UserV1DTO {

    public record UserRequest(String userId, String email, String birthdate, String gender) {

    }
    public record UserResponse(Long id, String userId, String email, String birthdate, String gender) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.id(),
                    userInfo.userId(),
                    userInfo.email(),
                    userInfo.birthdate(),
                    userInfo.gender()
            );
        }
    }
}
