package com.loopers.application.order;

import com.loopers.domain.order.Delivery;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Arrays;
import java.util.List;

public class OrderCommand {

    public enum PaymentMethod {
        POINT("point"),
        CARD("card");

        private final String value;

        PaymentMethod(String value) {
            this.value = value;
        }

        public static PaymentMethod fromValue(String value) {
            if (value == null || value.isBlank()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다.");
            }
            return Arrays.stream(PaymentMethod.values())
                    .filter(method -> method.value.equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, 
                            String.format("유효하지 않은 결제 수단입니다: %s", value)));
        }
    }

    public record CreateOrderCommand(
            String loginId,
            List<OrderItemCommand> orderItems,
            Long couponId,
            DeliveryCommand delivery,
            PaymentMethod paymentMethod
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
