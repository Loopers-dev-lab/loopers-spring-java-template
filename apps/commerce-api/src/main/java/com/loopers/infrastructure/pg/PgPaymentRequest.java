package com.loopers.infrastructure.pg;

public record PgPaymentRequest(
        String orderId,
        String cardType,
        String cardNo,
        String amount,
        String callbackUrl
) {
    public static PgPaymentRequest of(String orderId, String cardType, String cardNo,
                                      String amount, String callbackUrl) {
        return new PgPaymentRequest(orderId, cardType, cardNo, amount, callbackUrl);
    }
}
