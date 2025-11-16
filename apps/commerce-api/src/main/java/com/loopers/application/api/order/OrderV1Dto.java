package com.loopers.application.api.order;

import com.loopers.core.service.order.command.OrderProductsCommand;

import java.util.List;

public class OrderV1Dto {

    public record OrderRequest(
            List<OrderItemRequest> items
    ) {

        public OrderProductsCommand toCommand(String userIdentifier) {
            return new OrderProductsCommand(
                    userIdentifier,
                    this.items.stream()
                            .map(OrderItemRequest::toCommand)
                            .toList()
            );
        }

        public record OrderItemRequest(
                String productId,
                Long quantity
        ) {

            public OrderProductsCommand.OrderItem toCommand() {
                return new OrderProductsCommand.OrderItem(productId, quantity);
            }
        }
    }

    public record OrderResponse(
            String orderId
    ) {
    }
}
