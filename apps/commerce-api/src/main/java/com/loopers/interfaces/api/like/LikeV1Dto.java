package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;

public class LikeV1Dto {
    public record LikedProductResponse(
            Long userId,
            Long productId,
            int likeCount
    ) {
        public static LikedProductResponse from(LikeInfo info) {
            return new LikedProductResponse(
                info.userId(),
                info.productId(),
                info.likeCount()
            );
        }
    }
}
