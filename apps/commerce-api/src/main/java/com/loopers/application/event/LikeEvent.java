package com.loopers.application.event;

public record LikeEvent(
    Long userId,
    Long productId,
    LikeAction action // LIKE, UNLIKE
) {
    public enum LikeAction {
        LIKE,
        UNLIKE
    }
}
