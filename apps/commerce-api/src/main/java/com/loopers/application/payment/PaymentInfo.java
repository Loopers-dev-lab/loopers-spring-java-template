package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentDto;
import lombok.Builder;

/**
 * Payment Application Layer Info
 */
@Builder
public record PaymentInfo(
        Long orderId,
        String transactionKey,
        PaymentDto.PaymentStatus status,
        String reason
) {}

