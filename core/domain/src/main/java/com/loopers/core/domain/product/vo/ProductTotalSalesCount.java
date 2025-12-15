package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.order.vo.Quantity;

public record ProductTotalSalesCount(Long value) {

    public static ProductTotalSalesCount init() {
        return new ProductTotalSalesCount(0L);
    }

    public ProductTotalSalesCount increase(Quantity quantity) {
        return new ProductTotalSalesCount(quantity.plus(this.value));
    }
}
