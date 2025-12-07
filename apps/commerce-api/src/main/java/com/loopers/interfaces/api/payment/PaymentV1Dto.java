package com.loopers.interfaces.api.payment;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;

public class PaymentV1Dto {

    public record CardPaymentInfo (
            String cardType,
            String cardNo
    ) {
    }

    public record PointPaymentInfo () {
    }

    public record PgPaymentRequest(
        Long orderId,
        String cardType,
        String cardNo,
        Money amount,
        String callbackUrl
    ) {
        public static PgPaymentRequest of(Order order, CardPaymentInfo cardInfo) {
            return new PgPaymentRequest(
                    order.getId(),
                    cardInfo.cardType,
                    cardInfo.cardNo(),
                    order.calculateTotalPrice(),
                    "http://localhost:8080/api/v1/payments/callback"
            );
        }
    }

    public enum pgPaymentStatus {
        SUCCESS, FAILURE
    }

    public record PgPaymentResponse(
            String transactionKey,
            Long orderId,
            pgPaymentStatus result,
            String reason
    ) {

    }
}
