package com.loopers.application.payment;

public class PaymentCommand {

    public record RequestPaymentCommand(
            String loginId,
            String orderKey,
            String cardType,
            String cardNo
    ) {
    }
}



