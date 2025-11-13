package com.loopers.application.product;

import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductInfo(Long id, Long brandId, String name, BigDecimal price, int stock, int likeCount) {
    public static ProductInfo from(Product product, int likeCount) {
        return new ProductInfo(
            product.getId(),
            product.getBrandId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            likeCount
        );
    }
}
