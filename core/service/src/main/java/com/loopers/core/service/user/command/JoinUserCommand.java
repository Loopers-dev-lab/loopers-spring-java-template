package com.loopers.core.service.user.command;

import lombok.Getter;

@Getter
public class JoinUserCommand {

    private final String userIdentifier;

    private final String email;

    private final String birthDay;

    private final String gender;

    public JoinUserCommand(String userIdentifier, String email, String birthDay, String gender) {
        this.userIdentifier = userIdentifier;
        this.email = email;
        this.birthDay = birthDay;
        this.gender = gender;
    }
}
