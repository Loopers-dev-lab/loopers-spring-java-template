package com.loopers.domain.payment.event;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.loopers.domain.payment.PaymentStatus;

/**
 * 결제 데이터 플랫폼 전송 이벤트
 * <p>
 * 결제 완료/실패 시 데이터 플랫폼으로 전송할 이벤트
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record PaymentDataPlatformEvent(
        String transactionKey,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal amount,
        String cardType,
        ZonedDateTime eventTime,
        String eventType, // "PAYMENT_COMPLETED", "PAYMENT_FAILED", "PAYMENT_TIMEOUT"
        String failureReason
) {

    public static PaymentDataPlatformEvent completed(
            String transactionKey,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String cardType
    ) {
        return new PaymentDataPlatformEvent(
                transactionKey,
                orderId,
                userId,
                PaymentStatus.COMPLETED,
                amount,
                cardType,
                ZonedDateTime.now(),
                "PAYMENT_COMPLETED",
                null
        );
    }

    public static PaymentDataPlatformEvent failed(
            String transactionKey,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String cardType,
            String reason
    ) {
        return new PaymentDataPlatformEvent(
                transactionKey,
                orderId,
                userId,
                PaymentStatus.FAILED,
                amount,
                cardType,
                ZonedDateTime.now(),
                "PAYMENT_FAILED",
                reason
        );
    }

    public static PaymentDataPlatformEvent timeout(
            String transactionKey,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String cardType
    ) {
        return new PaymentDataPlatformEvent(
                transactionKey,
                orderId,
                userId,
                PaymentStatus.TIMEOUT,
                amount,
                cardType,
                ZonedDateTime.now(),
                "PAYMENT_TIMEOUT",
                "결제 콜백 타임아웃 (10분 초과)"
        );
    }
}
