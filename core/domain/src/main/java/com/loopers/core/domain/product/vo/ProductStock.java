package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.order.vo.Quantity;

import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.PRODUCT_OUT_OF_STOCK;

public record ProductStock(Long value) {

    private static final String FIELD_NAME = "상품 재고";

    public ProductStock {
        validateNotNull(value);
        validateNotNegative(value);
    }

    public static ProductStock init() {
        return new ProductStock(0L);
    }

    private static void validateNotNull(Long value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage(FIELD_NAME));
    }

    private static void validateNotNegative(Long value) {
        if (value < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FIELD_NAME));
        }
    }

    public ProductStock decrease(Quantity quantity) {
        if (this.value < quantity.value()) {
            throw new IllegalArgumentException(PRODUCT_OUT_OF_STOCK.getMessage());
        }

        return new ProductStock(this.value - quantity.value());
    }
}
