package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.order.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {

    public record CreateOrderRequest(
        @NotEmpty(message = "주문 항목은 비어있을 수 없습니다.")
        List<OrderItemRequest> orderItems,
        Long userCouponId
    ) {
    }

    public record OrderItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,
        @Positive(message = "수량은 1개 이상이어야 합니다.")
        int quantity
    ) {
    }

    public record OrderResponse(
        Long id,
        String userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> orderItems,
        ZonedDateTime createdAt,
        ZonedDateTime canceledAt
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                info.id(),
                info.userId(),
                info.status(),
                info.totalAmount(),
                info.orderItems().stream()
                    .map(OrderItemResponse::from)
                    .toList(),
                info.createdAt(),
                info.canceledAt()
            );
        }
    }

    public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String brandName,
        BigDecimal price,
        int quantity,
        BigDecimal amount
    ) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                info.id(),
                info.productId(),
                info.productName(),
                info.brandName(),
                info.price(),
                info.quantity(),
                info.amount()
            );
        }
    }
}
