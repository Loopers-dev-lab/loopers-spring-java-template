package com.loopers.core.domain.common.vo;

import java.time.LocalDateTime;

public record DeletedAt(LocalDateTime value) {

    public static DeletedAt now() {
        return new DeletedAt(LocalDateTime.now());
    }
}
