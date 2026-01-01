package com.loopers.core.domain.product.vo;

public record ProductRanking(
        ProductId productId,
        Long ranking,
        Double score
) {
}
