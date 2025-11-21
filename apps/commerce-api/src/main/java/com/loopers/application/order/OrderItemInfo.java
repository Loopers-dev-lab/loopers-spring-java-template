package com.loopers.application.order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.order.OrderItemEntity;

/**
 * 주문 항목 정보 DTO
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderItemInfo(
        Long id,
        Long orderId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {

    /**
     * OrderItemEntity로부터 OrderItemInfo를 생성합니다.
     *
     * @param orderItem 주문 항목 엔티티
     * @return OrderItemInfo
     */
    public static OrderItemInfo from(OrderItemEntity orderItem) {
        return new OrderItemInfo(
                orderItem.getId(),
                orderItem.getOrderId(),
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getTotalPrice(),
                orderItem.getCreatedAt(),
                orderItem.getUpdatedAt()
        );
    }
}
