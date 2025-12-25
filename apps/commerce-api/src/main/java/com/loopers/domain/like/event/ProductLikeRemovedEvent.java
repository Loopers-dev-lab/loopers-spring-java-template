package com.loopers.domain.like.event;

public record ProductLikeRemovedEvent(
        Long likeId,
        Long productId
) {
    public static ProductLikeRemovedEvent of(
            Long likeId,
            Long productId
    ) {
        return new ProductLikeRemovedEvent(likeId, productId);
    }
}
