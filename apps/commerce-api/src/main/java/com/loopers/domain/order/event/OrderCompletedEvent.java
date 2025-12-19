package com.loopers.domain.order.event;

import com.loopers.domain.order.Order;

import java.time.LocalDateTime;

public class OrderCompletedEvent {

    private final Long orderId;
    private final String userId;
    private final long totalAmount;
    private final long discountAmount;
    private final long paymentAmount;
    private final LocalDateTime occurredAt;

    private OrderCompletedEvent(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.totalAmount = order.getTotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.paymentAmount = order.getPaymentAmount();
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

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
