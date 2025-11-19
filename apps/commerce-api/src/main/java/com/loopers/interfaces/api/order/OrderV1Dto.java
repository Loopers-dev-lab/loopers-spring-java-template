package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public class OrderV1Dto {

    public record OrderRequest(
            List<OrderItemRequest> items
    ) {
        public record OrderItemRequest(
                Long productId,
                Integer quantity
        ) {}
    }

    public record OrderResponse(Long id, OrderStatus status, BigDecimal totalPrice) {
        public static OrderResponse from(OrderInfo orderInfo) {
            return new OrderResponse(
                    orderInfo.orderId(),
                    orderInfo.status(),
                    orderInfo.totalPrice()
            );
        }
    }
}
