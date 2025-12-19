package com.loopers.domain.payment;

public class PaymentEvent {
    public record PaymentPaid(Payment payment) {
        public static PaymentPaid from(final Payment payment) {
            return new PaymentPaid(payment);
        }
    }
    public record PaymentFailed(Payment payment) {
        public static PaymentFailed from(final Payment payment) {
            return new PaymentFailed(payment);
        }
    }
}
