package com.loopers.application.purchasing;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

/**
 * 주문 정보를 담는 레코드.
 *
 * @param orderId 주문 ID
 * @param userId 사용자 ID
 * @param totalAmount 총 주문 금액
 * @param status 주문 상태
 * @param items 주문 아이템 목록
 */
public record OrderInfo(
    Long orderId,
    Long userId,
    Integer totalAmount,
    OrderStatus status,
    List<OrderItemInfo> items
) {
    /**
     * Order 엔티티로부터 OrderInfo를 생성합니다.
     *
     * @param order 주문 엔티티
     * @return 생성된 OrderInfo
     */
    public static OrderInfo from(Order order) {
        List<OrderItemInfo> itemInfos = order.getItems() == null
            ? List.of()
            : order.getItems().stream()
                .map(OrderItemInfo::from)
                .toList();

        return new OrderInfo(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getStatus(),
            itemInfos
        );
    }
}

