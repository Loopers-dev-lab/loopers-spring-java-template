package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {

    @Builder
    public record OrderItemRequest(
            Long productId,
            Integer quantity
    ) {}

    @Builder
    public record CreateOrderRequest(
            List<OrderItemRequest> items
            , List<Long> couponIds  // 쿠폰 ID 리스트
    ) {}

    @Builder
    public record OrderResponse(
            Long id,
            BigDecimal finalAmount,
            BigDecimal totalPrice,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            String orderStatus,
            String userLoginId,
            List<OrderItemResponse> orderItems
    ) {
        public static OrderResponse from(OrderInfo orderInfo) {
            return OrderResponse.builder()
                    .id(orderInfo.id())
                    .finalAmount(orderInfo.finalAmount())
                    .totalPrice(orderInfo.totalPrice())
                    .discountAmount(orderInfo.discountAmount())
                    .shippingFee(orderInfo.shippingFee())
                    .orderStatus(orderInfo.orderStatus().name())
                    .userLoginId(orderInfo.userLoginId())
                    .orderItems(orderInfo.orderItems().stream()
                            .map(OrderItemResponse::from)
                            .toList())
                    .build();
        }
    }

    @Builder
    public record OrderItemResponse(
            Long id,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Long productId,
            String productName
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo orderItemInfo) {
            return OrderItemResponse.builder()
                    .id(orderItemInfo.id())
                    .quantity(orderItemInfo.quantity())
                    .unitPrice(orderItemInfo.unitPrice())
                    .totalAmount(orderItemInfo.totalAmount())
                    .productId(orderItemInfo.productId())
                    .productName(orderItemInfo.productName())
                    .build();
        }
    }
}

