package com.loopers.infrastructure.pg.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PgPaymentRequest {
    private final String orderId;
    private final String cardType;
    private final String cardNo;
    private final Long amount;
    private final String callbackUrl;
}