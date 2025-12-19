package com.loopers.domain.dataplatform;

import java.time.ZonedDateTime;

public record PaymentData(
        String orderKey,
        String transactionKey,
        Integer amount,
        String paymentStatus,
        String reason,
        ZonedDateTime createdAt
) {
}

