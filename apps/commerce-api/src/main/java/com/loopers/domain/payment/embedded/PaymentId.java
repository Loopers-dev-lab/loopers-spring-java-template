package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentId {
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    public PaymentId() {}

    private PaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public static PaymentId of(Long paymentId) {
        validatePaymentId(paymentId);
        return new PaymentId(paymentId);
    }

    public Long getValue() {
        return paymentId;
    }

    private static void validatePaymentId(Long paymentId) {
        if (paymentId == null || paymentId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 결제 ID입니다.");
        }
    }

}
