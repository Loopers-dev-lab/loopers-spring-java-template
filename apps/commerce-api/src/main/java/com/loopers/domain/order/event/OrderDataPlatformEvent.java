package com.loopers.domain.order.event;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.order.OrderStatus;

/**
 * 주문 데이터 플랫폼 전송 이벤트
 * <p>
 * 주문 확정/취소 시 데이터 플랫폼으로 전송할 이벤트
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record OrderDataPlatformEvent(
        Long orderId,
        Long orderNumber,
        Long userId,
        OrderStatus status,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount,
        ZonedDateTime eventTime,
        String eventType // "ORDER_CONFIRMED", "ORDER_CANCELLED"
) {

    public static OrderDataPlatformEvent confirmed(
            Long orderId,
            Long orderNumber,
            Long userId,
            BigDecimal originalTotalAmount,
            BigDecimal discountAmount,
            BigDecimal finalTotalAmount
    ) {
        return new OrderDataPlatformEvent(
                orderId,
                orderNumber,
                userId,
                OrderStatus.CONFIRMED,
                originalTotalAmount,
                discountAmount,
                finalTotalAmount,
                ZonedDateTime.now(),
                "ORDER_CONFIRMED"
        );
    }

    public static OrderDataPlatformEvent cancelled(
            Long orderId,
            Long orderNumber,
            Long userId,
            BigDecimal originalTotalAmount,
            BigDecimal discountAmount,
            BigDecimal finalTotalAmount,
            String reason
    ) {
        return new OrderDataPlatformEvent(
                orderId,
                orderNumber,
                userId,
                OrderStatus.CANCELLED,
                originalTotalAmount,
                discountAmount,
                finalTotalAmount,
                ZonedDateTime.now(),
                "ORDER_CANCELLED"
        );
    }
}
