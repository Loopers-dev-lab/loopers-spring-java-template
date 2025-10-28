package com.loopers.core.service.user.query;

import lombok.Getter;

@Getter
public class GetUserQuery {

    private final String identifier;

    public GetUserQuery(String identifier) {
        this.identifier = identifier;
    }
}
