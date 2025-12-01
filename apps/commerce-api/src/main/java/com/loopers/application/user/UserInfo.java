package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(String userId, String email, String birth, String gender) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getBirth(),
                user.getGender()
        );
    }
}
