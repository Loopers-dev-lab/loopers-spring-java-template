package com.loopers.infrastructure.dataplatform.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.event.PaymentDataPlatformEvent;

/**
 * 데이터 플랫폼 전송용 결제 데이터 DTO
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record PaymentDataDto(
        String transactionKey,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal amount,
        String cardType,
        ZonedDateTime eventTime,
        String eventType,
        String failureReason
) {

    public static PaymentDataDto from(PaymentDataPlatformEvent event) {
        return new PaymentDataDto(
                event.transactionKey(),
                event.orderId(),
                event.userId(),
                event.status(),
                event.amount(),
                event.cardType(),
                event.eventTime(),
                event.eventType(),
                event.failureReason()
        );
    }
}
