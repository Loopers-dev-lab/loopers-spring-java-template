package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public class OrderV1Dto {

    public record OrderResponse(
            Long orderId,
            List<OrderItem> items,
            Integer totalPrice
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                    info.orderId(),
                    OrderItem.fromList(info.items()),
                    info.totalPrice()
            );
        }
    }

    public record OrderItem(
            Long productId,
            String productName,
            Integer quantity,
            Integer totalPrice
    ) {
        public static OrderItem from(OrderItemInfo info) {
            return new OrderItem(
                    info.productId(),
                    info.productName(),
                    info.quantity(),
                    info.totalPrice()
            );
        }

        public static List<OrderItem> fromList(List<OrderItemInfo> infos) {
            return infos.stream()
                    .map(OrderItem::from)
                    .toList();
        }
    }

    public record OrderPageResponse(
            List<OrderResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {
        public static OrderPageResponse from(Page<OrderInfo> page) {
            return new OrderPageResponse(
                    page.map(OrderResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getNumberOfElements()
            );
        }
    }
}
