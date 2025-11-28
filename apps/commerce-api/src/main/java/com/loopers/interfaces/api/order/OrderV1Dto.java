package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {
    public record PlaceOrderRequest(
            @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
            @Valid
            List<OrderItemRequest> items,

            Long couponId
    ) {
        public OrderPlaceCommand toCommand(String userId) {
            List<OrderPlaceCommand.OrderItemCommand> itemCommands = items.stream()
                    .map(item -> new OrderPlaceCommand.OrderItemCommand(item.productId(), item.quantity()))
                    .toList();
            return new OrderPlaceCommand(userId, itemCommands, couponId);
        }
    }

    public record OrderItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,

            @NotNull(message = "수량은 필수입니다.")
            @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
            Integer quantity
    ) {
    }

    public record OrderResponse(
            Long orderId,
            Long totalAmount,
            OrderStatus status,
            ZonedDateTime paidAt,
            List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> itemResponses = info.items().stream()
                    .map(OrderItemResponse::from)
                    .toList();

            return new OrderResponse(
                    info.orderId(),
                    info.totalAmount(),
                    info.status(),
                    info.paidAt(),
                    itemResponses
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            Long unitPrice,
            Integer quantity,
            Long totalPrice
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo info) {
            return new OrderItemResponse(
                    info.productId(),
                    info.productName(),
                    info.unitPrice(),
                    info.quantity(),
                    info.totalPrice()
            );
        }
    }
}
