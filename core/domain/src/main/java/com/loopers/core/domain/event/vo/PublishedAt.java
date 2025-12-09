package com.loopers.core.domain.event.vo;

import java.time.LocalDateTime;

public record PublishedAt(LocalDateTime value) {

    public static PublishedAt empty() {
        return new PublishedAt(null);
    }

    public static PublishedAt now() {
        return new PublishedAt(LocalDateTime.now());
    }
}
