package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;

import java.util.List;

public class OrderDto {

    public record OrderCreateRequest(
            List<OrderItemRequest> items
    ) {
    }

    public record OrderItemRequest(
            Long productId,
            Long quantity
    ) {
    }

    public record OrderResponse(
            Long orderId,
            String userId,
            List<OrderItemResponse> items,
            int totalAmount,
            String status
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                    info.orderId(),
                    info.userId(),
                    info.items().stream()
                            .map(item -> new OrderItemResponse(
                                    item.productId(),
                                    item.productName(),
                                    item.quantity(),
                                    item.price()
                            ))
                            .toList(),
                    info.totalAmount(),
                    info.status()
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            Long quantity,
            int price
    ) {
    }

    public record OrderListResponse(
            List<OrderResponse> orders
    ) {
        public static OrderListResponse from(List<OrderInfo> orders) {
            return new OrderListResponse(
                    orders.stream()
                            .map(OrderResponse::from)
                            .toList()
            );
        }
    }
}
