package com.loopers.infrastructure.dataplatform;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 데이터 플랫폼 주문 데이터 전송 요청
 */
public record DataPlatformOrderRequest(
    String orderId,
    String userId,
    String status,
    BigDecimal totalAmount,
    BigDecimal finalAmount,
    Long userCouponId,
    List<OrderItemData> items,
    ZonedDateTime createdAt
) {
    public record OrderItemData(
        String productId,
        Integer quantity,
        BigDecimal price
    ) {}
}
