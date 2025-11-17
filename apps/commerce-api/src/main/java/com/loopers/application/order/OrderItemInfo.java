package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record OrderItemInfo(
    Long id,
    Long productId,
    String productName,
    String brandName,
    Integer quantity,
    BigDecimal price,
    BigDecimal amount
) {

    public static OrderItemInfo from(OrderItem orderItem) {
        return OrderItemInfo.builder()
            .id(orderItem.getId())
            .productId(orderItem.getProductId())
            .productName(orderItem.getProductName())
            .brandName(orderItem.getBrandName())
            .quantity(orderItem.getQuantity())
            .price(orderItem.getPrice())
            .amount(orderItem.calculateAmount())
            .build();
    }
}
