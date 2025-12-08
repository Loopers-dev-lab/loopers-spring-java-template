package com.loopers.infrastructure.payment.client.dto;

import java.math.BigDecimal;
import java.util.Objects;

import com.loopers.application.payment.PaymentCommand;

/**
 * PG 결제 요청 DTO
 *
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
public record PgPaymentRequest(
        Long orderId,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {

    public static PgPaymentRequest of(PaymentCommand command) {
        Objects.requireNonNull(command, "결제 명령(PaymentCommand)이 null입니다.");
        Objects.requireNonNull(command.orderNumber(), "주문번호(orderNumber)가 null입니다.");
        Objects.requireNonNull(command.cardType(), "카드 타입(cardType)이 null입니다.");
        Objects.requireNonNull(command.cardNo(), "카드 번호(cardNo)가 null입니다.");
        Objects.requireNonNull(command.amount(), "결제 금액(amount)이 null입니다.");
        Objects.requireNonNull(command.callbackUrl(), "콜백 URL(callbackUrl)이 null입니다.");

        return new PgPaymentRequest(
                command.orderNumber(),
                command.cardType(),
                command.cardNo(),
                command.amount(),
                command.callbackUrl()
        );
    }
}
