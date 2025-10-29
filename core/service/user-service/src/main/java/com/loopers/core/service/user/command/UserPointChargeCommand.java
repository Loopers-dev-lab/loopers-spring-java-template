package com.loopers.core.service.user.command;

import lombok.Getter;

@Getter
public class UserPointChargeCommand {

    private final String userIdentifier;
    private final int point;

    public UserPointChargeCommand(String userIdentifier, int point) {
        this.userIdentifier = userIdentifier;
        this.point = point;
    }
}
