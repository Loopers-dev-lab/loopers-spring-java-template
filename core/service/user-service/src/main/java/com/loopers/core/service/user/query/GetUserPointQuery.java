package com.loopers.core.service.user.query;

import lombok.Getter;

@Getter
public class GetUserPointQuery {

    private final String userIdentifier;

    public GetUserPointQuery(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }
}
