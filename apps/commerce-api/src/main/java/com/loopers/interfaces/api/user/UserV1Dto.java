package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

public class UserV1Dto {

    public record UserCreateRequest(
            String userId,
            String userName,
            String description,
            String email,
            String birthDate,
            String gender
    ) { }


    public record UserResponse(
            String userId,
            String userName,
            String description,
            String email,
            String birthDate,
            String gender,
            Integer point
    ) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.userId(),
                    info.userName(),
                    info.description(),
                    info.email(),
                    info.birthDate(),
                    info.gender(),
                    info.point()
            );
        }
    }
}
