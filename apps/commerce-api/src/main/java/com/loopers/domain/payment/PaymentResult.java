package com.loopers.domain.payment;

public record PaymentResult(String transactionId, String status, String message) {

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
