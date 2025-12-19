package com.loopers.application.like;

public class LikeEvent {

    public record LikeRecorded(
            Long productId
    ) {
        public static LikeRecorded from(Long productId) {
            return new LikeRecorded(productId);
        }
    }

    public record LikeCancelled(
            Long productId
    ) {
        public static LikeCancelled from(Long productId) {
            return new LikeCancelled(productId);
        }
    }
}
