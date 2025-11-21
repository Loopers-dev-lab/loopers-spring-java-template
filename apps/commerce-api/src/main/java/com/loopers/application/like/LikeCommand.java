package com.loopers.application.like;

public record LikeCommand(
        String userId,
        Long productId
) {
}
