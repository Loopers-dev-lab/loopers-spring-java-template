package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;

import java.util.List;

public record OrderInfoDetail(
        Long orderId,
        Long userId,
        Integer totalPrice,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String paymentId,
        String pgPaymentReason,
        List<OrderItemInfo> items
) {
    public static OrderInfoDetail from(Order order) {
        return new OrderInfoDetail(
                order.getId(),
                order.getUserId(),
                order.getFinalPrice().amount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPgTransactionKey(),
                order.getPgPaymentReason(),
                OrderItemInfo.fromList(order.getOrderItems())
        );
    }
}
