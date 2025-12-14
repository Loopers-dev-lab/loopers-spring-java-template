package com.loopers.core.domain.payment.vo;

import com.loopers.core.domain.event.vo.AggregateId;

public record PaymentId(String value) {

    public static PaymentId empty() {
        return new PaymentId(null);
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }
}
