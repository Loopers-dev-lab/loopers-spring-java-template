package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.product.vo.ProductTotalSalesCount;

import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.negativeMessage;
import static com.loopers.core.domain.error.DomainErrorCode.notNullMessage;

public record Quantity(Long value) {

    private static final String FIELD_NAME = "주문 개수";

    public Quantity {
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

    public ProductTotalSalesCount plusTotalSalesCount(ProductTotalSalesCount totalSalesCount) {
        return totalSalesCount.plus(this.value);
    }
}
