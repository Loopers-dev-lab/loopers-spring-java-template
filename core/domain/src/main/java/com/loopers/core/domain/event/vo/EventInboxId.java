package com.loopers.core.domain.event.vo;

public record EventInboxId(String value) {

    public static EventInboxId empty() {
        return new EventInboxId(null);
    }
}
