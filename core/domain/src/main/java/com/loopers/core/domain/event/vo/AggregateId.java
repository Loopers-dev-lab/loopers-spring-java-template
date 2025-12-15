package com.loopers.core.domain.event.vo;

import com.loopers.core.domain.product.vo.ProductId;

public record AggregateId(String value) {

    public ProductId toProductId() {
        return new ProductId(value);
    }
}
