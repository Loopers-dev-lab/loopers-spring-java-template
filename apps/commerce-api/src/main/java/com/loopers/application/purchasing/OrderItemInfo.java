package com.loopers.application.purchasing;

import com.loopers.domain.order.OrderItem;

/**
 * 주문 아이템 정보를 담는 레코드.
 *
 * @param productId 상품 ID
 * @param name 상품 이름
 * @param price 상품 가격
 * @param quantity 수량
 */
public record OrderItemInfo(
    Long productId,
    String name,
    Integer price,
    Integer quantity
) {
    /**
     * OrderItem으로부터 OrderItemInfo를 생성합니다.
     *
     * @param item 주문 아이템
     * @return 생성된 OrderItemInfo
     */
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
            item.getProductId(),
            item.getName(),
            item.getPrice(),
            item.getQuantity()
        );
    }
}

