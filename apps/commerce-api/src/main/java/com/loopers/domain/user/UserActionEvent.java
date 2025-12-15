package com.loopers.domain.user;

import java.time.ZonedDateTime;
import java.util.Map;

public record UserActionEvent(
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
        PRODUCT_UNLIKE,
        ORDER_CREATE,
        ORDER_COMPLETE,
        ORDER_CANCEL,
        PAYMENT_SUCCESS,
        PAYMENT_FAIL
    }

    public static UserActionEvent of(
            Long userId,
            ActionType actionType,
            String targetType,
            Long targetId,
            Map<String, Object> metadata
    ) {
        return new UserActionEvent(
                userId,
                actionType,
                targetType,
                targetId,
                metadata,
                ZonedDateTime.now()
        );
    }

    public static UserActionEvent productView(Long userId, Long productId) {
        return of(userId, ActionType.PRODUCT_VIEW, "PRODUCT", productId, Map.of());
    }

    public static UserActionEvent productLike(Long userId, Long productId) {
        return of(userId, ActionType.PRODUCT_LIKE, "PRODUCT", productId, Map.of());
    }

    public static UserActionEvent productUnlike(Long userId, Long productId) {
        return of(userId, ActionType.PRODUCT_UNLIKE, "PRODUCT", productId, Map.of());
    }

    public static UserActionEvent orderCreate(Long userId, Long orderId, Long totalAmount) {
        return of(userId, ActionType.ORDER_CREATE, "ORDER", orderId,
                Map.of("totalAmount", totalAmount));
    }

    public static UserActionEvent orderComplete(Long userId, Long orderId) {
        return of(userId, ActionType.ORDER_COMPLETE, "ORDER", orderId, Map.of());
    }

    public static UserActionEvent orderCancel(Long userId, Long orderId) {
        return of(userId, ActionType.ORDER_CANCEL, "ORDER", orderId, Map.of());
    }

    public static UserActionEvent paymentSuccess(Long userId, Long paymentId, Long amount) {
        return of(userId, ActionType.PAYMENT_SUCCESS, "PAYMENT", paymentId,
                Map.of("amount", amount));
    }

    public static UserActionEvent paymentFail(Long userId, Long orderId, String reason) {
        return of(userId, ActionType.PAYMENT_FAIL, "ORDER", orderId,
                Map.of("reason", reason));
    }
}
