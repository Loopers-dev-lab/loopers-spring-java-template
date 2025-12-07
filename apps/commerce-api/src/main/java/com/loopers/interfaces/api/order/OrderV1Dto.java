package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.interfaces.api.payment.PaymentMethod;
import com.loopers.interfaces.api.payment.PaymentV1Dto;

import java.util.List;

public class OrderV1Dto {

    public record OrderCreateRequest(
            List<OrderItemInfo> items,
            PaymentMethod payment,
            PaymentV1Dto.CardPaymentInfo cardPaymentInfo
    ) {
        public static OrderCreateRequest of(
                List<OrderItemInfo> items,
                PaymentMethod payment,
                PaymentV1Dto.CardPaymentInfo cardPaymentInfo
        ) {
            return new OrderCreateRequest(
                    items,
                    payment,
                    cardPaymentInfo
            );
        }
    }

    public record OrderCreateResponse(
            Long id,
            OrderStatus status
    ) {
        public static OrderCreateResponse from(Order order) {
            return new OrderCreateResponse(
                    order.getId(),
                    order.getStatus()
            );
        }
    }

    public record OrderSummaryResponse(
            Long id,
            Money totalAmount,
            OrderStatus status
    ) {
        public static OrderSummaryResponse from(Order order) {
            return new OrderSummaryResponse(
                    order.getId(),
                    order.calculateTotalPrice(),
                    order.getStatus()
            );
        }
    }

    public record OrderListResponse(
            List<OrderSummaryResponse> orders,
            int totalCount
    ) {
        public static OrderListResponse from(List<Order> orders) {
            return new OrderListResponse(
                    orders.stream().map(OrderSummaryResponse::from).toList(),
                    orders.size()
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            int quantity,
            Money unitPrice,
            Money totalPrice
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()
            );
        }
    }

    public record OrderDetailResponse(
            Long id,
            OrderStatus status,
            List<OrderItemResponse> item
    ) {
        public static OrderDetailResponse from(Order order) {
            return new OrderDetailResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getItems().stream().map(OrderItemResponse::from).toList()
            );
        }
    }
}
