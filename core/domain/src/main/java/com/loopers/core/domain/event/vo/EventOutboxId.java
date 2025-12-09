package com.loopers.core.domain.event.vo;

public record EventOutboxId(String value) {

    public static EventOutboxId empty() {
        return new EventOutboxId(null);
    }
}
