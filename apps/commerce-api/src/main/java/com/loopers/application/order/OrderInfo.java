package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.OrderItem;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        Long totalAmount,
        Long discountAmount,
        OrderStatus status,
        ZonedDateTime paidAt,
        List<OrderItemInfo> items,
        String paymentMethod,
        Long paymentId
) {
    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {
        public static OrderItemInfo from(OrderItem item) {
            return new OrderItemInfo(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPriceValue(),
                    item.calculateAmount().getValue()
            );
        }
    }

    public static OrderInfo from(Order order, Long discountAmount) {
        return new OrderInfo(
                order.getId(),
                order.getUser().getUserIdValue(),
                order.getTotalAmountValue(),
                discountAmount != null ? discountAmount : 0L,
                order.getStatus(),
                order.getPaidAt(),
                order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList(),
                null,
                null
        );
    }

    public static OrderInfo from(Order order, Long discountAmount, String paymentMethod, Long paymentId) {
        return new OrderInfo(
                order.getId(),
                order.getUser().getUserIdValue(),
                order.getTotalAmountValue(),
                discountAmount != null ? discountAmount : 0L,
                order.getStatus(),
                order.getPaidAt(),
                order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList(),
                paymentMethod,
                paymentId
        );
    }

    public Long getFinalAmount() {
        return totalAmount - discountAmount;
    }
}
