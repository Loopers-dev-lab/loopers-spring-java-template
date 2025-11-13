package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;
import com.loopers.domain.like.Like;

public class LikeV1Dto {
    public record LikeResponse(Long id, Long userId, Long productId) {
        public static LikeResponse from(LikeInfo info) {
            return new LikeResponse(
                    info.id(),
                    info.userId(),
                    info.productId()
            );
        }
    }

    public record LikeRequest(Long userId, Long productId) {
        public Like toEntity() {
            return new Like(
                    userId,
                    productId
            );
        }
    }

    public record UnLikeRequest(Long userId, Long productId) {
        public Like toEntity() {
            return new Like(
                    userId,
                    productId
            );
        }
    }
}
