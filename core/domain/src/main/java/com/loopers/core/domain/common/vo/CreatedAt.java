package com.loopers.core.domain.common.vo;

import java.time.LocalDateTime;

public record CreatedAt(LocalDateTime value) {

    public static CreatedAt now() {
        return new CreatedAt(LocalDateTime.now());
    }
}
