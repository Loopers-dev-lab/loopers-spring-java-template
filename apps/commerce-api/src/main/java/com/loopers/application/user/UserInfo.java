package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(String id, String email, String birthday, String gender) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getBirthday(),
                user.getGender()
        );
    }
}
