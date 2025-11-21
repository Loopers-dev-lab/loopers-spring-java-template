package com.loopers.application.order;

import com.loopers.domain.order.Delivery;
import java.util.List;

public class OrderCommand {

    public record CreateOrderCommand(
            String loginId,
            List<OrderItemCommand> orderItems,
            Long couponId,
            DeliveryCommand delivery
    ) {
    }

    public record OrderItemCommand(
            Long productId,
            Integer quantity
    ) {
    }

    public record DeliveryCommand(
            String receiverName,
            String receiverPhoneNumber,
            String address,
            String addressDetail
    ) {
        public static Delivery toDelivery(DeliveryCommand deliveryCommand) {
            return Delivery.createDelivery(
                    deliveryCommand.receiverName(),
                    deliveryCommand.receiverPhoneNumber(),
                    deliveryCommand.address(),
                    deliveryCommand.addressDetail()
            );
        }
    }
}
