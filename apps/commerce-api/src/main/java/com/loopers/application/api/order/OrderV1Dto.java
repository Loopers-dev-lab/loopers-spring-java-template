package com.loopers.application.api.order;

import com.loopers.core.domain.order.OrderDetail;
import com.loopers.core.domain.order.OrderItem;
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

    public record OrderDetailResponse(
            String orderId,
            String userId,
            List<OrderDetailItemResponse> items
    ) {

        public static OrderDetailResponse from(OrderDetail detail) {
            return new OrderDetailResponse(
                    detail.getOrder().getId().value(),
                    detail.getOrder().getUserId().value(),
                    detail.getOrderItems().stream()
                            .map(OrderDetailItemResponse::from)
                            .toList()
            );
        }

        public record OrderDetailItemResponse(
                String productId,
                Long quantity
        ) {
            public static OrderDetailItemResponse from(OrderItem item) {
                return new OrderDetailItemResponse(
                        item.getProductId().value(),
                        item.getQuantity().value()
                );
            }
        }
    }
}
