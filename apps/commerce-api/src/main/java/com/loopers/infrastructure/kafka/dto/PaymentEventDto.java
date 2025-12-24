package com.loopers.infrastructure.kafka.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEventDto(
        String eventId,
        Long orderId,
        Long userId,
        String paymentStatus,
        String transactionId,
        Long amount,
        String failureReason,
        LocalDateTime occurredAt
) {
    public static PaymentEventDto success(Long orderId, Long userId, String transactionId, Long amount) {
        return new PaymentEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "SUCCESS",
                transactionId,
                amount,
                null,
                LocalDateTime.now()
        );
    }

    public static PaymentEventDto failed(Long orderId, Long userId, String failureReason) {
        return new PaymentEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "FAILED",
                null,
                null,
                failureReason,
                LocalDateTime.now()
        );
    }

    public static PaymentEventDto pending(Long orderId, Long userId, String transactionId) {
        return new PaymentEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "PENDING",
                transactionId,
                null,
                null,
                LocalDateTime.now()
        );
    }
}
