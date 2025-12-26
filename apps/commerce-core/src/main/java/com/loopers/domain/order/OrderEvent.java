package com.loopers.domain.order;

import com.loopers.domain.common.vo.Price;
import com.loopers.domain.event.BaseEvent;
import com.loopers.domain.event.EventType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class OrderEvent {
    @Getter
    public static class OrderCreatedEvent extends BaseEvent {
        private final Long orderId;
        private final String orderPublicId;
        private final Long userId;
        private final Long couponId;
        private final Price finalPrice;
        private final PaymentMethod paymentMethod;

        private OrderCreatedEvent(
                EventType eventType,
                Long orderId,
                String orderPublicId,
                Long userId,
                Long couponId,
                Price finalPrice,
                PaymentMethod paymentMethod
        ) {
            super(eventType);
            if (orderId == null) {
                throw new IllegalArgumentException("주문 ID는 필수입니다.");
            }
            if (orderPublicId == null) {
                throw new IllegalArgumentException("주문 공개 ID는 필수입니다.");
            }
            if (userId == null) {
                throw new IllegalArgumentException("사용자 ID는 필수입니다.");
            }
            if (finalPrice == null) {
                throw new IllegalArgumentException("최종 금액은 필수입니다.");
            }
            if (paymentMethod == null) {
                throw new IllegalArgumentException("결제 수단은 필수입니다.");
            }
            this.orderId = orderId;
            this.orderPublicId = orderPublicId;
            this.userId = userId;
            this.couponId = couponId;
            this.finalPrice = finalPrice;
            this.paymentMethod = paymentMethod;
        }
    }

    public static OrderCreatedEvent createOrderCreatedEvent(
            Long orderId,
            String orderPublicId,
            Long userId,
            Long couponId,
            Price finalPrice,
            PaymentMethod paymentMethod
    ) {
        return new OrderCreatedEvent(
                EventType.CREATED,
                orderId,
                orderPublicId,
                userId,
                couponId,
                finalPrice,
                paymentMethod
        );
    }

    @Getter
    public static class OrderPaidEvent extends BaseEvent {
        private final Long orderId;
        private final String orderPublicId;
        private final Long userId;
        private final Long couponId;
        private final OrderStatus orderStatus;
        private final PaymentMethod paymentMethod;
        private final String transactionKey;
        private final Price finalPrice;
        private final String statusMessage;

        private OrderPaidEvent(
                EventType eventType,
                Long orderId,
                String orderPublicId,
                Long userId,
                Long couponId,
                OrderStatus orderStatus,
                PaymentMethod paymentMethod,
                String transactionKey,
                Price finalPrice,
                String statusMessage) {
            super(eventType);
            if (orderId == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
            }
            if (StringUtils.isBlank(orderPublicId)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 공개 ID는 필수입니다.");
            }
            if (userId == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
            }
            if (orderStatus == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태는 필수입니다.");
            }
            if (paymentMethod == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다.");
            }
            if (paymentMethod == PaymentMethod.PG && StringUtils.isBlank(transactionKey)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "PG 결제의 경우 거래 키는 필수입니다.");
            }
            if (finalPrice == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "최종 금액은 필수입니다.");
            }
            this.orderId = orderId;
            this.orderPublicId = orderPublicId;
            this.userId = userId;
            this.couponId = couponId;
            this.orderStatus = orderStatus;
            this.paymentMethod = paymentMethod;
            this.transactionKey = transactionKey;
            this.finalPrice = finalPrice;
            this.statusMessage = statusMessage;
        }
    }

    public static OrderPaidEvent createOrderPaidEvent(EventType eventType, Order order) {
        return new OrderPaidEvent(
                eventType,
                order.getId(),
                order.getOrderId(),
                order.getUserId(),
                order.getCouponId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPgTransactionKey(),
                order.getFinalPrice(),
                order.getPgPaymentReason()
        );
    }
}
