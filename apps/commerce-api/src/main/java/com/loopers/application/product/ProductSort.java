package com.loopers.application.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ProductSort {
    LATEST("latest", "최신순"),
    PRICE_ASC("price_asc", "가격 오름차순"),
    LIKES_DESC("likes_desc", "좋아요 내림차순");

    private final String value;
    private final String description;

    ProductSort(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ProductSort fromValue(String value) {
        if (value == null) {
            return LATEST;
        }
        return Arrays.stream(ProductSort.values())
                .filter(sort -> sort.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 정렬 기준입니다: " + value));
    }
}

