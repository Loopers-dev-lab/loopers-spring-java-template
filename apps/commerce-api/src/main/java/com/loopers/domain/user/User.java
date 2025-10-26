package com.loopers.domain.user;

import lombok.Getter;

@Getter
public class User {

    private final UserId userId;
    private final UserEmail email;
    private final UserBirth birth;

    public User(UserId userId, UserEmail email, UserBirth birth) {
        this.userId = userId;
        this.email = email;
        this.birth = birth;
    }
}
