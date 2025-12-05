package com.loopers.application.payment;

public record PaymentCallbackDto(
        String transactionId,
        String status,
        String message
) {
    public static PaymentCallbackDto from(String transactionId, String status, String message) {
        return new PaymentCallbackDto(transactionId, status, message);
    }
}
