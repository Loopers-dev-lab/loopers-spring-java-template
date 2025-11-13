package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.common.Money;
import com.loopers.domain.order.OrderItemModel;
import java.util.List;

public class OrderV1Dto {
    public record OrderItemResponse(Long productId, Integer quantity, Money price) {
        public static OrderItemResponse from(OrderItemModel item) {
            return new OrderItemResponse(
                item.getProduct().getId(),
                item.getQuantity().quantity(),
                item.getOrderPrice()
            );
        }
    }

    public record OrderResponse(Long id, String userId, Money totalPrice, List<OrderItemResponse> orderItems) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> items = info.orderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
            return new OrderResponse(
                info.id(),
                info.user().getUserId().userId(),
                info.totalPrice(),
                items
            );
        }
    }

    public record OrdersResponse(List<OrderResponse> orders) {
        public static OrdersResponse from(List<OrderInfo> orders) {
            List<OrderResponse> orderResponses = orders.stream()
                .map(OrderResponse::from)
                .toList();
            return new OrdersResponse(orderResponses);
        }
    }

    public record CreateOrderRequest(List<OrderItemRequest> items) {
        public record OrderItemRequest(Long productId, Integer quantity) {}
    }
}

