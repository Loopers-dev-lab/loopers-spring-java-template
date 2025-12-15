package com.loopers.core.domain.event.vo;

import java.util.UUID;

public record EventId(String value) {

    public static EventId generate() {
        return new EventId(
                UUID.randomUUID().toString()
                        .replaceAll("-", "")
                        .substring(0, 12)
        );
    }
}
