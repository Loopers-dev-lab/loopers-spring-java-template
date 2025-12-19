package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

public class PaymentEvent {

    public record PaymentStatusUpdateRequest(
            String orderKey,
            PaymentStatus status,
            String transactionKey
    ) {
        public static PaymentStatusUpdateRequest completed(String orderKey, String transactionKey) {
            return new PaymentStatusUpdateRequest(orderKey, PaymentStatus.COMPLETED, transactionKey);
        }

        public static PaymentStatusUpdateRequest failed(String orderKey) {
            return new PaymentStatusUpdateRequest(orderKey, PaymentStatus.FAILED, null);
        }
    }

    public record PaymentCompleted(
            String orderKey,
            String transactionKey,
            Integer amount
    ) {
        public static PaymentCompleted from(Payment payment) {
            return new PaymentCompleted(
                    payment.getOrderKey(),
                    payment.getTransactionKey(),
                    payment.getAmount()
            );
        }
    }

    public record PaymentFailed(
            String orderKey,
            String reason
    ) {
        public static PaymentFailed from(Payment payment, String reason) {
            return new PaymentFailed(
                    payment.getOrderKey(),
                    reason
            );
        }
    }
}
