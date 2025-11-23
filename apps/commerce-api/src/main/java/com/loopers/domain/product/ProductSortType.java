package com.loopers.domain.product;

import org.springframework.data.domain.Sort;

public enum ProductSortType {
    LATEST(Sort.by("createdAt").descending()),
    PRICE_ASC(Sort.by("price.amount").ascending()),
    LIKES_DESC(Sort.by("likeCount").descending());

    private final Sort sort;

    ProductSortType(Sort sort) {
        this.sort = sort;
    }

    public Sort getSort() {
        return sort;
    }

    public static Sort toSort (String value) {
        if(value == null || value.isBlank()) {
            return LATEST.getSort();
        }
        return switch (value.toLowerCase()) {
            case "price_asc" -> PRICE_ASC.getSort();
            case "like_desc" -> LIKES_DESC.getSort();
            default -> LATEST.getSort();
        };
    }
}
