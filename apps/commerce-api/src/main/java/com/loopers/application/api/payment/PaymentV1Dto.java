package com.loopers.application.api.payment;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.service.payment.command.PaymentCommand;
import jakarta.validation.constraints.NotBlank;

public class PaymentV1Dto {

    public record PaymentRequest(
            @NotBlank String cardType,
            @NotBlank String cardNo,
            String couponId
    ) {
        public PaymentCommand toCommand(String orderId, String userIdentifier) {
            return new PaymentCommand(orderId, userIdentifier, cardType, cardNo, couponId);
        }
    }

    public record PaymentResponse(
            String paymentId
    ) {

        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId().value());
        }
    }
}
