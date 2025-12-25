package com.loopers.domain.like.event;


public record ProductLikeAddedEvent(
        Long likeId,
        Long productId
) {
    public static ProductLikeAddedEvent of(
            Long likeId,
            Long productId
    ) {
        return new ProductLikeAddedEvent(likeId, productId);
    }
}
