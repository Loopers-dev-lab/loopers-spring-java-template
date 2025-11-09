package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record ProductLikeCount(Long value) {

    public ProductLikeCount(Long value) {
        validateNotNull(value);
        validateNotNegative(value);

        this.value = value;
    }

    public static ProductLikeCount init() {
        return new ProductLikeCount(0L);
    }

    private static void validateNotNull(Long value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("상품 좋아요 수"));
    }

    private static void validateNotNegative(Long value) {
        if (value < 0) {
            throw new IllegalArgumentException(DomainErrorCode.COULD_NOT_BE_PRODUCT_LIKE_COUNT_NEGATIVE.getMessage());
        }
    }

    public ProductLikeCount increase() {
        return new ProductLikeCount(this.value + 1);
    }

    public ProductLikeCount decrease() {
        return new ProductLikeCount(this.value - 1);
    }
}
