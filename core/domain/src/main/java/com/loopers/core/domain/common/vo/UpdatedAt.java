package com.loopers.core.domain.common.vo;

import java.time.LocalDateTime;

public record UpdatedAt(LocalDateTime value) {

    public static UpdatedAt now() {
        return new UpdatedAt(LocalDateTime.now());
    }
}
