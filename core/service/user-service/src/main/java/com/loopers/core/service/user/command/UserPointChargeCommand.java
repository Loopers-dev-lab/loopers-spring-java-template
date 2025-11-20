package com.loopers.core.service.user.command;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UserPointChargeCommand {

    private final String userIdentifier;
    private final BigDecimal point;

    public UserPointChargeCommand(String userIdentifier, BigDecimal point) {
        this.userIdentifier = userIdentifier;
        this.point = point;
    }
}
