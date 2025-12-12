package com.loopers.domain.like;

public record LikeEvent(
        Long userId,
        Long productId,
        LikeAction action
) {
    public enum LikeAction {
        ADDED,
        REMOVED
    }

    public static LikeEvent added(Long userId, Long productId) {
        return new LikeEvent(userId, productId, LikeAction.ADDED);
    }

    public static LikeEvent removed(Long userId, Long productId) {
        return new LikeEvent(userId, productId, LikeAction.REMOVED);
    }
}
