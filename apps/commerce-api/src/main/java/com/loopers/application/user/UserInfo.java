package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(Long id, String name, String description) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getId(),
            model.getName(),
            model.getDescription()
        );
    }
}
