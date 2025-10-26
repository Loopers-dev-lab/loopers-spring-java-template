package com.loopers.core.service.user.command;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateUserCommand {

    private final String userIdentifier;

    private final String email;

    private final LocalDateTime birthDay;

    private final String gender;

    public CreateUserCommand(String userIdentifier, String email, LocalDateTime birthDay, String gender) {
        this.userIdentifier = userIdentifier;
        this.email = email;
        this.birthDay = birthDay;
        this.gender = gender;
    }
}
