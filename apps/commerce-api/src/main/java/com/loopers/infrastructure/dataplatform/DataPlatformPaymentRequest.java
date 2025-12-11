package com.loopers.infrastructure.dataplatform;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * 데이터 플랫폼 결제 데이터 전송 요청
 */
public record DataPlatformPaymentRequest(
    String paymentId,
    Long orderId,
    String userId,
    BigDecimal amount,
    String status,
    String transactionKey,
    ZonedDateTime createdAt
) {}
