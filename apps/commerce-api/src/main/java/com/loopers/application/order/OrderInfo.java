package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderInfo(
        Long id,
        BigDecimal finalAmount,
        BigDecimal totalPrice,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        OrderStatus orderStatus,
        String userLoginId,
        List<OrderItemInfo> orderItems
) {
    public static OrderInfo from(Order order) {
        return OrderInfo.builder()
                .id(order.getId())
                .finalAmount(order.getFinalAmount())
                .totalPrice(order.getTotalPrice())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .orderStatus(order.getOrderStatus())
                .orderItems(order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList())
                .build();
    }

    @Builder
    public record OrderItemInfo(
            Long id,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Long productId,
            String productName
    ) {
        public static OrderItemInfo from(OrderItem orderItem) {
            return OrderItemInfo.builder()
                    .id(orderItem.getId())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getProductPrice()) // productPrice를 unitPrice로 매핑
                    .totalAmount(orderItem.getTotalAmount())
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .build();
        }
    }
}

