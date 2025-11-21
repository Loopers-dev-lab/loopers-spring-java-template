package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.common.Money;
import com.loopers.application.order.OrderItemInfo;
import java.util.List;

public class OrderV1Dto {
    public record OrderItemResponse(Long productId, Integer quantity, Money price) {
        public static OrderItemResponse from(OrderItemInfo item) {
            return new OrderItemResponse(
                item.productId(),
                item.quantity(),
                item.orderPrice()
            );
        }
    }

    public record OrderResponse(Long id, String userId, Money totalPrice, List<OrderItemResponse> orderItems) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> items = info.orderItems().stream()
                .map(item -> OrderItemResponse.from(item))
                .toList();
            return new OrderResponse(
                info.id(),
                info.userId().toString(),
                info.totalPrice(),
                items
            );
        }
    }

    public record OrdersResponse(List<OrderResponse> orders) {
        public static OrdersResponse from(List<OrderInfo> orders) {
            List<OrderResponse> orderResponses = orders.stream()
                .map(info -> OrderResponse.from(info))
                .toList();
            return new OrdersResponse(orderResponses);
        }
    }

    public record CreateOrderRequest(List<OrderItemRequest> items) {
        public record OrderItemRequest(Long productId, Integer quantity) {}
    }
}

