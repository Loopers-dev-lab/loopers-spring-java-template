package com.loopers.infrastructure.platform;

import java.time.ZonedDateTime;
import java.util.Map;

public record UserActionMessage(
        Long userId,
        ActionType actionType,
        String targetType,
        Long targetId,
        Map<String, Object> metadata,
        ZonedDateTime occurredAt
) {
    public enum ActionType {
        PRODUCT_VIEW,
        PRODUCT_LIKE,
        PRODUCT_UNLIKE
    }

    public static UserActionMessage productLike(Long userId, Long productId) {
        return new UserActionMessage(
                userId,
                ActionType.PRODUCT_LIKE, "PRODUCT", productId,
                Map.of(), ZonedDateTime.now()
        );
    }

    public static UserActionMessage productUnlike(Long userId, Long productId) {
        return new UserActionMessage(
                userId,
                ActionType.PRODUCT_UNLIKE, "PRODUCT", productId,
                Map.of(), ZonedDateTime.now()
        );
    }

    public static UserActionMessage productView(Long userId, Long productId) {
        return new UserActionMessage(
                userId,
                ActionType.PRODUCT_VIEW, "PRODUCT", productId,
                Map.of(), ZonedDateTime.now()
        );
    }
}
