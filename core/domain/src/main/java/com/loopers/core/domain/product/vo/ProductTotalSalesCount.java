package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.order.vo.Quantity;

import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.negativeMessage;
import static com.loopers.core.domain.error.DomainErrorCode.notNullMessage;

public record ProductTotalSalesCount(Long value) {

    private static final String FIELD_NAME = "전체 판매량";

    public ProductTotalSalesCount {
        validateNull(value);
        validateNegative(value);
    }

    private static void validateNegative(Long value) {
        if (value < 0) {
            throw new IllegalArgumentException(negativeMessage(FIELD_NAME));
        }
    }

    private static void validateNull(Long value) {
        if (Objects.isNull(value)) {
            throw new NullPointerException(notNullMessage(FIELD_NAME));
        }
    }

    public static ProductTotalSalesCount init() {
        return new ProductTotalSalesCount(0L);
    }

    public ProductTotalSalesCount increase(Quantity quantity) {
        return quantity.plusTotalSalesCount(this);
    }

    public ProductTotalSalesCount plus(Long value) {
        return new ProductTotalSalesCount(this.value + value);
    }
}
