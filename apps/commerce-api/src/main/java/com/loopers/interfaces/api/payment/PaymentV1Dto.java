package com.loopers.interfaces.api.payment;

public class PaymentV1Dto {

    public record PaymentCallbackRequest(
            String transactionKey,
            String status,
            String reason,
            String callbackUrl
    ) {
    }
}
