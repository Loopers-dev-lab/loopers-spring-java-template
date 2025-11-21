package com.loopers.application.order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderStatus;

/**
 * 주문 정보 DTO
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderInfo(
        Long id,
        Long userId,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount,
        OrderStatus status,
        List<OrderItemInfo> orderItems,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {

    /**
     * OrderEntity로부터 OrderInfo를 생성합니다.
     *
     * @param order      주문 엔티티
     * @param orderItems 주문 항목 엔티티 목록
     * @return OrderInfo
     */
    public static OrderInfo from(OrderEntity order, List<OrderItemEntity> orderItems) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getOriginalTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalTotalAmount(),
                order.getStatus(),
                orderItems.stream()
                        .map(OrderItemInfo::from)
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
