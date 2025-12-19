package com.loopers.domain.like.event;

import java.time.LocalDateTime;

public class ProductLikedEvent {

    private final Long productId;
    private final Long userId;
    private final boolean liked;
    private final LocalDateTime occurredAt;

    private ProductLikedEvent(Long productId, Long userId, boolean liked) {
        this.productId = productId;
        this.userId = userId;
        this.liked = liked;
        this.occurredAt = LocalDateTime.now();
    }

    public static ProductLikedEvent liked(Long productId, Long userId) {
        return new ProductLikedEvent(productId, userId, true);
    }

    public static ProductLikedEvent unliked(Long productId, Long userId) {
        return new ProductLikedEvent(productId, userId, false);
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isLiked() {
        return liked;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}