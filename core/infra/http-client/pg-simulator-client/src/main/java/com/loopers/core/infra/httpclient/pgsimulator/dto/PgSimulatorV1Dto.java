package com.loopers.core.infra.httpclient.pgsimulator.dto;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;

public class PgSimulatorV1Dto {

    public record PgSimulatorPaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            long amount,
            String callbackUrl
    ) {
        public static PgSimulatorPaymentRequest from(Payment payment, String callbackUrl) {
            return new PgSimulatorPaymentRequest(
                    payment.getOrderKey().value(),
                    payment.getCardType().value(),
                    payment.getCardNo().value(),
                    payment.getAmount().value().longValue(),
                    callbackUrl
            );
        }
    }

    public record PgSimulatorPaymentResponse(
            Data data
    ) {

        public PgPayment to() {
            return this.data.to();
        }

        public record Data(
                String transactionKey,
                String status,
                String reason
        ) {
            public PgPayment to() {
                return new PgPayment(
                        new TransactionKey(this.transactionKey),
                        PaymentStatus.PENDING,
                        FailedReason.empty()
                );
            }
        }
    }
}
