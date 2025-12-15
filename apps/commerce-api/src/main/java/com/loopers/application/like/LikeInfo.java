package com.loopers.application.like;

public record LikeInfo(
        Long userId,
        Long productId,
        int likeCount
) {
}
