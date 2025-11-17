package com.loopers.application.api.order;

import com.loopers.core.domain.order.OrderListItem;
import com.loopers.core.domain.order.OrderListView;
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

    public record OrderListResponse(
            List<OrderListItemResponse> items,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static OrderListResponse from(OrderListView view) {
            return new OrderListResponse(
                    view.getItems().stream()
                            .map(OrderListItemResponse::from)
                            .toList(),
                    view.getTotalElements(),
                    view.getTotalPages(),
                    view.isHasNext(),
                    view.isHasPrevious()
            );
        }

        public record OrderListItemResponse(
                String orderId,
                String userId
        ) {
            public static OrderListItemResponse from(OrderListItem item) {
                return new OrderListItemResponse(
                        item.getOrderId().value(),
                        item.getUserId().value()
                );
            }
        }
    }
}
