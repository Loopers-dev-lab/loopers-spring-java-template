package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.event.vo.AggregateId;

public record OrderId(String value) {

    public static OrderId empty() {
        return new OrderId(null);
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }
}
