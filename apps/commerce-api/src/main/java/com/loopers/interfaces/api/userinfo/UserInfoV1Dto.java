package com.loopers.interfaces.api.userinfo;

import com.loopers.domain.user.User;

public class UserInfoV1Dto {
    public record UserInfoResponse(
        String userId,
        String email,
        String birthDate,
        String gender
    ) {
        public static UserInfoResponse from(User user) {
            return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate().toString(),
                user.getGender() != null ? user.getGender().name() : null
            );
        }
    }
}
