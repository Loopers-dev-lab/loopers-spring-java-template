package com.loopers.interfaces.api.payment;

public record PaymentCallbackRequest(
    String transactionKey,
    String orderId,
    String status,
    String reason
) {
}
