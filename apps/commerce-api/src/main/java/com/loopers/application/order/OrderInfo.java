package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;

import java.util.List;

public record OrderInfo(
        Long orderId,
        Long userId,
        Integer totalPrice,
        List<OrderItemInfo> items,
        PaymentMethod paymentMethod,
        OrderStatus status,
        PaymentInfo payment,
        Integer retryCount,
        Boolean canRetry
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getFinalPrice().amount(),
                OrderItemInfo.fromList(order.getOrderItems()),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getStatus() != null && order.getStatus() != OrderStatus.PENDING
                    ? PaymentInfo.from(order)
                    : null,
                order.getRetryCount(),
                order.getPaymentMethod() == PaymentMethod.PG ? order.canRetryPayment() : null
        );
    }

    /**
     * PG 트랜잭션 키 조회 (헬퍼 메서드)
     */
    public String getPgTransactionKey() {
        return payment != null ? payment.pgPaymentId() : null;
    }

    /**
     * PG 결제 실패 사유 조회 (헬퍼 메서드)
     */
    public String getPgPaymentReason() {
        return payment != null ? payment.pgPaymentReason() : null;
    }
}

record PaymentInfo(
        String pgPaymentId,
        OrderStatus status,
        String pgPaymentReason
) {
    static PaymentInfo from(Order order) {
        return new PaymentInfo(
                order.getPgTransactionKey(),
                order.getStatus(),
                order.getPgPaymentReason()
        );
    }
}
