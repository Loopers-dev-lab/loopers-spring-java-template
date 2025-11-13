package com.loopers.application.like;

import com.loopers.domain.like.Like;

public record LikeInfo(Long id, Long userId, Long productId) {
    public static LikeInfo from(Like like) {
        return new LikeInfo(
                like.getId(),
                like.getUserId(),
                like.getProductId()
        );
    }
}
