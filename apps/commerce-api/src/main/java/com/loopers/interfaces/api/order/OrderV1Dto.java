package com.loopers.interfaces.api.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public class OrderV1Dto {
    public record CreateOrderRequest(List<OrderItemRequest> items) {}
    public record OrderItemRequest(Long productId, int quantity, BigDecimal price) {}

    public record OrderResponse(Long id, String status, int itemCount) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getOrderItems().size()
            );
        }
    }

    public record OrderSummary(Long id, String status, int itemCount) {
        public static OrderSummary from(Order order) {
            return new OrderSummary(
                order.getId(),
                order.getStatus().name(),
                order.getOrderItems().size()
            );
        }
    }

    public record OrderListResponse(List<OrderSummary> items) {}
}
