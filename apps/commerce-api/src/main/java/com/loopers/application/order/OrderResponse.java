package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 응답 DTO
 */
public class OrderResponse {
    private final Long orderId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final int usedPoints;
    private final BigDecimal finalAmount;
    private final OrderStatus status;
    private final LocalDateTime orderedAt;
    private final LocalDateTime modifiedAt;
    private final List<OrderItemResponse> orderItems;

    private OrderResponse(Long orderId, String userId, BigDecimal totalAmount, int usedPoints,
                          BigDecimal finalAmount, OrderStatus status, LocalDateTime orderedAt,
                          LocalDateTime modifiedAt, List<OrderItemResponse> orderItems) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.status = status;
        this.orderedAt = orderedAt;
        this.modifiedAt = modifiedAt;
        this.orderItems = orderItems;
    }

    /**
     * Order 도메인 객체로부터 DTO 생성
     */
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getModifiedAt(),
                itemResponses
        );
    }

    // === Getters ===

    public Long getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getUsedPoints() {
        return usedPoints;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    /**
     * 주문 항목 응답 DTO
     */
    public static class OrderItemResponse {
        private final Long orderItemId;
        private final Long productId;
        private final String productName;
        private final BigDecimal price;
        private final int quantity;
        private final BigDecimal subtotal;

        private OrderItemResponse(Long orderItemId, Long productId, String productName,
                                  BigDecimal price, int quantity, BigDecimal subtotal) {
            this.orderItemId = orderItemId;
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public static OrderItemResponse from(OrderItem orderItem) {
            return new OrderItemResponse(
                    orderItem.getId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getPrice(),
                    orderItem.getQuantity(),
                    orderItem.getSubtotal()
            );
        }

        public Long getOrderItemId() {
            return orderItemId;
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }
}
