package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {

    public record PlaceOrderRequest(
            @NotEmpty @Valid List<OrderLineRequest> orderLines
    ) {}

    public record OrderLineRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record OrderResponse(
            Long id,
            String memberId,
            BigDecimal totalPrice,
            List<OrderItemResponse> items,
            ZonedDateTime orderedAt
    ) {
        public static OrderResponse from(OrderInfo orderInfo) {
            List<OrderItemResponse> itemResponses = orderInfo.getItems().stream()
                    .map(OrderItemResponse::from)
                    .toList();

            return new OrderResponse(
                    orderInfo.getId(),
                    orderInfo.getMemberId(),
                    orderInfo.getTotalPrice().getAmount(),
                    itemResponses,
                    orderInfo.getOrderedAt()
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo itemInfo) {
            return new OrderItemResponse(
                    itemInfo.getProductId(),
                    itemInfo.getQuantity(),
                    itemInfo.getUnitPrice().getAmount(),
                    itemInfo.getTotalPrice().getAmount()
            );
        }
    }
}