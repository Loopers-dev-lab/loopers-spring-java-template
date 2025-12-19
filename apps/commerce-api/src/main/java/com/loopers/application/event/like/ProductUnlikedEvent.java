package com.loopers.application.event.like;

import java.time.LocalDateTime;

public record ProductUnlikedEvent(
    Long memberId,
    Long productId,
    Long brandId,
    LocalDateTime unlikedAt
) {
}
