package com.loopers.domain.payment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.user.UserEntity;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

/**
 * @author hyunjikoh
 * @since 2025. 12. 5.
 */
public class PaymentDomainDtos {
    public record PaymentDomainCreateRequest(
            Long userId,
            Long orderNumber,
            String transactionKey,
            String cardType,
            String cardNo,
            String callbackUrl,
            BigDecimal amount,
            PaymentStatus paymentStatus,
            ZonedDateTime requestedAt,
            String failureReason
    ) {
        public static PaymentDomainCreateRequest from(UserEntity user, PaymentCommand command, PgPaymentResponse pgResponse) {
            return new PaymentDomainCreateRequest(
                    user.getId(),
                    command.orderId(),
                    pgResponse.transactionKey(),
                    command.cardType(),
                    command.cardNo(),
                    command.callbackUrl(),
                    command.amount(),
                    PaymentStatus.fromString(pgResponse.status()),
                    ZonedDateTime.now(),
                    pgResponse.reason()
            );
        }
    }


    public record PgPaymentResult(
            String transactionKey,
            String status,
            String failureReason,
            boolean isApiFail,
            String errorCode,
            String message) {
        public static PgPaymentResult from(PgPaymentResponse pgResponse) {
            return new PgPaymentResult(
                    pgResponse.data().transactionKey(),
                    pgResponse.data().status(),
                    pgResponse.data().reason(),
                    pgResponse.meta().isFail(),
                    pgResponse.meta().errorCode(),
                    pgResponse.meta().message()
            );
        }
    }

}
