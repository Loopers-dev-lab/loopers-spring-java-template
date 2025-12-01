package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

/**
 * packageName : com.loopers.application.order
 * fileName     : OrderInfo
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
public record OrderItemInfo(
        Long productId,
        String productName,
        Long quantity,
        Long price,
        Long amount
) {
    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getAmount()
        );
    }
}
