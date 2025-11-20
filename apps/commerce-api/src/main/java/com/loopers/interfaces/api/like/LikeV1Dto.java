package com.loopers.interfaces.api.like;

import com.loopers.domain.like.Like;

public class LikeV1Dto {

    public record LikeResponse(
        Long id,
        String userId,
        Long productId
    ) {
        public static LikeResponse from(Like like) {
            return new LikeResponse(
                like.getId(),
                like.getUserId(),
                like.getProductId()
            );
        }
    }

    public record LikeCountResponse(
        Long productId,
        Long likeCount
    ) {
        public static LikeCountResponse of(Long productId, Long likeCount) {
            return new LikeCountResponse(productId, likeCount);
        }
    }

    public record IsLikedResponse(
        String userId,
        Long productId,
        boolean isLiked
    ) {
        public static IsLikedResponse of(String userId, Long productId, boolean isLiked) {
            return new IsLikedResponse(userId, productId, isLiked);
        }
    }
}
