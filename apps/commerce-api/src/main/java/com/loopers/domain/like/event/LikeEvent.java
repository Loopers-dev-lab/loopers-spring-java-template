package com.loopers.domain.like.event;

import java.util.Objects;

public final class LikeEvent {

    private LikeEvent() {
    }

    public record ProductLiked(String userId, Long productId) {

        public ProductLiked {
            Objects.requireNonNull(userId, "userId 는 null 일 수 없습니다.");
            Objects.requireNonNull(productId, "productId 는 null 일 수 없습니다.");
        }

        public static ProductLiked of(String userId, Long productId) {
            return new ProductLiked(userId, productId);
        }
    }

    public record ProductUnliked(String userId, Long productId) {

        public ProductUnliked {
            Objects.requireNonNull(userId, "userId 는 null 일 수 없습니다.");
            Objects.requireNonNull(productId, "productId 는 null 일 수 없습니다.");
        }

        public static ProductUnliked of(String userId, Long productId) {
            return new ProductUnliked(userId, productId);
        }
    }
}
