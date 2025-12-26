package com.loopers.application.event.like;

import java.time.LocalDateTime;

public record ProductLikedEvent(
    Long memberId,
    Long productId,
    Long brandId,
    LocalDateTime likedAt
) {
}
