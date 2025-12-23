package com.loopers.application.like;

public class LikeEvent {

    public record LikeRecorded(
            Long productId,
            Long userId
    ) {
        public static LikeRecorded from(Long productId, Long userId) {
            return new LikeRecorded(productId, userId);
        }
    }

    public record LikeCancelled(
            Long productId,
            Long userId
    ) {
        public static LikeCancelled from(Long productId, Long userId) {
            return new LikeCancelled(productId, userId);
        }
    }
}
