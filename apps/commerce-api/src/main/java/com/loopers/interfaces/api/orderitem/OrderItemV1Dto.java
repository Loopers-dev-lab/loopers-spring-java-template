package com.loopers.interfaces.api.orderitem;

import com.loopers.application.orderitem.OrderItemInfo;
import com.loopers.domain.orderitem.OrderItem;

import java.math.BigDecimal;

public class OrderItemV1Dto {
    public record OrderItemResponse(
            Long id,
            Long orderId,
            Long productId,
            int quantity,
            BigDecimal orderPrice
    ) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                    info.id(),
                    info.orderId(),
                    info.productId(),
                    info.quantity(),
                    info.orderPrice()
            );
        }
    }

    public record OrderItemRequest(Long orderId, Long productId, int quantity, BigDecimal productPrice) {
        public OrderItem toEntity() {
            return new OrderItem(
                    orderId,
                    productId,
                    quantity,
                    productPrice.multiply(BigDecimal.valueOf(quantity))
            );
        }
    }
}
