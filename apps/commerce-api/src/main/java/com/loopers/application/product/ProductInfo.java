package com.loopers.application.product;

import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductInfo(
        Long productId,
        String productName,
        BigDecimal price,
        int likeCount
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getPrice().getAmount(),
                product.getLikeCount()
        );
    }
}
