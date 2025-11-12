package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public enum ProductSortType {
    LATEST("latest"),
    PRICE_ASC("price_asc"),
    LIKES_DESC("likes_desc");

    private final String value;

    ProductSortType(String value) {
        this.value = value;
    }

    public static ProductSortType from(String value) {
        if (value == null) {
            return LATEST;  // 기본값
        }

        for (ProductSortType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }

        throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 정렬 기준입니다.");
    }

    public String getValue() {
        return value;
    }
}
