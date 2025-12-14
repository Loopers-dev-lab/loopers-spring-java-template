package com.loopers.infrastructure.dataplatform.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderDataPlatformEvent;

/**
 * 데이터 플랫폼 전송용 주문 데이터 DTO
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record OrderDataDto(
        Long orderId,
        Long orderNumber,
        Long userId,
        OrderStatus status,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount,
        ZonedDateTime eventTime,
        String eventType
) {

    public static OrderDataDto from(OrderDataPlatformEvent event) {
        return new OrderDataDto(
                event.orderId(),
                event.orderNumber(),
                event.userId(),
                event.status(),
                event.originalTotalAmount(),
                event.discountAmount(),
                event.finalTotalAmount(),
                event.eventTime(),
                event.eventType()
        );
    }
}
