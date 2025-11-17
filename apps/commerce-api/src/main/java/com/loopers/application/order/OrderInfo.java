package com.loopers.application.order;

import com.loopers.domain.order.Delivery;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import java.util.List;

public record OrderInfo(
        Long orderId,
        String orderStatus,
        List<OrderItemInfo> orderItems,
        DeliveryInfo delivery
) {

    public static OrderInfo from(Order order, List<OrderItem> orderItems, Delivery delivery) {
        List<OrderItemInfo> itemInfos = orderItems.stream()
                .map(OrderItemInfo::from)
                .toList();

        return new OrderInfo(
                order.getId(),
                order.getOrderStatus().getDescription(),
                itemInfos,
                DeliveryInfo.from(delivery)
        );
    }

    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer quantity,
            Integer price
    ) {
        public static OrderItemInfo from(OrderItem orderItem) {
            return new OrderItemInfo(
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getQuantity(),
                    orderItem.getPrice()
            );
        }
    }

    public record DeliveryInfo(
            String receiverName,
            String receiverPhone,
            String address,
            String addressDetail
    ) {
        public static DeliveryInfo from(Delivery delivery) {
            return new DeliveryInfo(
                    delivery.getReceiverName(),
                    delivery.getReceiverPhoneNumber(),
                    delivery.getBaseAddress(),
                    delivery.getDetailAddress()
            );
        }
    }

}
