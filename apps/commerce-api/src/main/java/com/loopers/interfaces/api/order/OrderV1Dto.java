package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderCommand.CreateOrderCommand;
import com.loopers.application.order.OrderInfo;
import java.util.List;

public class OrderV1Dto {

    public record OrderRequest(
            List<OrderItemRequest> orderItems,
            DeliveryRequest delivery
    ) {
        public CreateOrderCommand toCommand(String loginId) {
            List<OrderCommand.OrderItemCommand> itemCommands = orderItems.stream()
                    .map(OrderItemRequest::toCommand)
                    .toList();

            return new OrderCommand.CreateOrderCommand(
                    loginId,
                    itemCommands,
                    delivery.toCommand()
            );
        }
    }

    public record OrderItemRequest(
            Long productId,
            Integer quantity
    ) {
        public OrderCommand.OrderItemCommand toCommand() {
            return new OrderCommand.OrderItemCommand(productId, quantity);
        }
    }

    public record DeliveryRequest(
            String receiverName,
            String receiverPhoneNumber,
            String address,
            String addressDetail
    ) {
        public OrderCommand.DeliveryCommand toCommand() {
            return new OrderCommand.DeliveryCommand(
                    receiverName,
                    receiverPhoneNumber,
                    address,
                    addressDetail
            );
        }
    }

    public record OrderResponse(
            Long orderId,
            String orderStatus,
            List<OrderItemResponse> orderItems,
            DeliveryResponse delivery
    ) {
        public static OrderResponse from(OrderInfo orderInfo) {
            return new OrderResponse(
                    orderInfo.orderId(),
                    orderInfo.orderStatus(),
                    orderInfo.orderItems().stream()
                            .map(OrderItemResponse::from)
                            .toList(),
                    DeliveryResponse.from(orderInfo.delivery())
            );
        }
    }

    public record  OrderItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            Integer price
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo itemInfo) {
            return new OrderItemResponse(
                    itemInfo.productId(),
                    itemInfo.productName(),
                    itemInfo.quantity(),
                    itemInfo.price()
            );
        }
    }

    public record DeliveryResponse(
            String receiverName,
            String receiverPhone,
            String address,
            String addressDetail
    ) {
        public static DeliveryResponse from(OrderInfo.DeliveryInfo deliveryInfo) {
            return new DeliveryResponse(
                    deliveryInfo.receiverName(),
                    deliveryInfo.receiverPhone(),
                    deliveryInfo.address(),
                    deliveryInfo.addressDetail()
            );
        }
    }
}
