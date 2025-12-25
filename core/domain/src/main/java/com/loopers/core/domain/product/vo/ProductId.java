package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.event.vo.AggregateId;

public record ProductId(String value) {

    public static ProductId empty() {
        return new ProductId(null);
    }

    public AggregateId toAggregateId() {
        return new AggregateId(value);
    }
}
