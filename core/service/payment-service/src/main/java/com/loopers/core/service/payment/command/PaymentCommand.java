package com.loopers.core.service.payment.command;

public record PaymentCommand(
        String orderId,
        String userIdentifier,
        String cardType,
        String cardNo,
        String couponId
) {

}
