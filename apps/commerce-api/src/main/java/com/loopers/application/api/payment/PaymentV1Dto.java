package com.loopers.application.api.payment;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.service.payment.command.PaymentCommand;
import com.loopers.core.service.payment.command.PgCallbackCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentV1Dto {

    public record PaymentRequest(
            String cardType,
            String cardNo,
            String couponId,
            String paymentType
    ) {
        public PaymentCommand toCommand(String orderId, String userIdentifier) {
            return new PaymentCommand(orderId, userIdentifier, cardType, cardNo, couponId, paymentType);
        }
    }

    public record PaymentResponse(
            String paymentId
    ) {

        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId().value());
        }
    }

    public record PgCallbackRequest(
            @NotBlank String transactionKey,
            @NotBlank String orderId,
            @NotBlank String cardType,
            @NotBlank String cardNo,
            @NotNull Long amount,
            @NotBlank String status,
            String reason
    ) {
        public PgCallbackCommand toCommand() {
            return new PgCallbackCommand(transactionKey, orderId, cardType, cardNo, amount, status, reason);
        }
    }
}
