package com.loopers.application.order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderStatus;

/**
 * 주문 목록 조회용 요약 정보 DTO
 * 주문 항목 정보를 포함하지 않아 가볍게 조회 가능
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderSummary(
        Long id,
        Long userId,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount,
        OrderStatus status,
        int itemCount,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {

    /**
     * OrderEntity로부터 OrderSummary를 생성합니다.
     *
     * @param order     주문 엔티티
     * @param itemCount 주문 항목 개수
     * @return OrderSummary
     */
    public static OrderSummary from(OrderEntity order, int itemCount) {
        return new OrderSummary(
                order.getId(),
                order.getUserId(),
                order.getOriginalTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalTotalAmount(),
                order.getStatus(),
                itemCount,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
