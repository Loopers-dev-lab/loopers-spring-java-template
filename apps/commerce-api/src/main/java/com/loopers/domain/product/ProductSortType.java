package com.loopers.domain.product;

public enum ProductSortType {
    LATEST("최신순"),
    PRICE_ASC("가격 낮은 순"),
    LIKES_DESC("좋아요 많은 순");

    private final String description;

    ProductSortType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
