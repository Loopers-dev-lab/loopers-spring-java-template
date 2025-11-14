package com.loopers.application.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.interfaces.api.user.UserDto;
import lombok.Builder;

@Builder
public record UserInfo(
        String loginId
        , String email
        , String birthday
        , Gender gender
        , Point point
) {
    public static UserInfo from(User user) {
        return UserInfo.builder()
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .point(user.getPoint())
                .build();
    }

    public static UserInfo from(UserDto.CreateUserRequest cur) {
        return UserInfo.builder()
                .loginId(cur.loginId())
                .email(cur.email())
                .birthday(cur.birthday())
                .gender(cur.gender())
                .build();
    }

}
