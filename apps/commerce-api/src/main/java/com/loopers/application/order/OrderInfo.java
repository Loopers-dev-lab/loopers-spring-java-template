package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * packageName : com.loopers.application.order
 * fileName     : OrderInfo
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
public record OrderInfo(
        Long orderId,
        String userId,
        Long totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemInfo> items
) {
    public static OrderInfo from(Order order) {
        List<OrderItemInfo> itemInfos = order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList();

        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemInfos
        );
    }
}
