package com.loopers.core.service.user.command;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class JoinUserCommand {

    private final String userIdentifier;

    private final String email;

    private final LocalDate birthDay;

    private final String gender;

    public JoinUserCommand(String userIdentifier, String email, LocalDate birthDay, String gender) {
        this.userIdentifier = userIdentifier;
        this.email = email;
        this.birthDay = birthDay;
        this.gender = gender;
    }
}
