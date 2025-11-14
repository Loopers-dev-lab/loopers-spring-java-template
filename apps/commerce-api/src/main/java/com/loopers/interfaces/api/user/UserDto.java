package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import lombok.Builder;

public class UserDto {

    @Builder
    public record CreateUserRequest(
            String loginId
            , String email
            , String birthday
            , Gender gender
    ) {}

    @Builder
    public record UserResponse(
            String loginId
            , String email
            , String birthday
            , Gender gender
    ) {
        public static UserResponse from(UserInfo userInfo) {
            return UserResponse.builder()
                    .loginId(userInfo.loginId())
                    .email(userInfo.email())
                    .birthday(userInfo.birthday())
                    .gender(userInfo.gender())
                    .build();
        }
    }

}
