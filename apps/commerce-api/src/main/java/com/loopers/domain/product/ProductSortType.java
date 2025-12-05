package com.loopers.domain.product;

import lombok.Getter;
import org.springframework.data.domain.Sort;

public enum ProductSortType {
    LATEST("latest", Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("price_asc", Sort.by(Sort.Direction.ASC, "price.value")),
    PRICE_DESC("price_desc", Sort.by(Sort.Direction.DESC, "price.value")),
    LIKES_DESC("likes_desc", Sort.by(Sort.Direction.DESC, "likeCount"));

    private final String code;

    @Getter
    private final Sort sort;

    ProductSortType(String code, Sort sort) {
        this.code = code;
        this.sort = sort;
    }

    public static ProductSortType from(String code) {
        if (code == null || code.isBlank()) {
            return LATEST;
        }

        for (ProductSortType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }

        return LATEST;
    }
}
