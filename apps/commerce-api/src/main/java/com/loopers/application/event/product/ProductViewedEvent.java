package com.loopers.application.event.product;

import java.time.LocalDateTime;

public record ProductViewedEvent(
    Long memberId,      // nullable (비로그인 사용자도 추적)
    Long productId,
    Long brandId,
    LocalDateTime viewedAt
) {
}