package com.loopers.domain.order.event;

import com.loopers.domain.order.Order;

import java.time.LocalDateTime;
import java.util.List;

public class OrderCompletedEvent {

    private Long orderId;
    private String userId;
    private long totalAmount;
    private long discountAmount;
    private long paymentAmount;
    private List<OrderItemInfo> items;
    private LocalDateTime occurredAt;

    protected OrderCompletedEvent() {
    }

    private OrderCompletedEvent(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.totalAmount = order.getTotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.paymentAmount = order.getPaymentAmount();
        this.items = order.getOrderItems().stream()
                .map(item -> new OrderItemInfo(item.getProductId(), item.getQuantity()))
                .toList();
        this.occurredAt = LocalDateTime.now();
    }

    public static OrderCompletedEvent from(Order order) {
        return new OrderCompletedEvent(order);
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getPaymentAmount() {
        return paymentAmount;
    }

    public List<OrderItemInfo> getItems() {
        return items;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public static class OrderItemInfo {
        private Long productId;
        private Long quantity;

        protected OrderItemInfo() {
        }

        public OrderItemInfo(Long productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public Long getQuantity() {
            return quantity;
        }
    }
}
