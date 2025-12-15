package com.loopers.interfaces.api.payment;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.payment.Payment;

import java.util.List;

public class PaymentV1Dto {

    public record CardPaymentInfo (
            String cardType,
            String cardNo
    ) {
        public static CardPaymentInfo from(Payment payment) {
            return new CardPaymentInfo(
                    payment.getCardType(),
                    payment.getCardNo()
            );
        }
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

        public static PgPaymentRequest from(Payment payment) {
            return new PgPaymentRequest(
                    payment.getOrderId(),
                    payment.getCardType(),
                    payment.getCardNo(),
                    payment.getAmount(),
                    "http://localhost:8080/api/v1/payments/callback"
            );
        }
    }

    public record PgResponse<T>(
            Meta meta,
            T data
    ) {
        public record Meta(
                String result,
                String errorCode,
                String message
        ) {}
    }

    public record PaymentResponse(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            int amount,
            String status,
            String reason
    ) {

    }

    public record PaymentHistoryResponse(
            String orderId,
            List<TransactionInfo> transactions
    ) {
        public record TransactionInfo(
                String transactionKey,
                String status,
                String reason
        ) {

        }
    }
}
