package com.loopers.application.like;

public record LikeInfo(
        Long productId,
        Integer likeCount
) {
    public static LikeInfo from(Long productId, Integer likeCount) {
        return new LikeInfo(productId, likeCount);
    }
}

