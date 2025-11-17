package com.loopers.application.product;

import com.loopers.domain.product.Product;

public record ProductInfo(
        Long id,
        String name,
        Long brandId,
        String brandName,
        Integer price,
        Integer likeCount,
        Integer stock
) {
    public static ProductInfo from(Product product, String brandName) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getBrandId(),
                brandName,
                product.getPrice().getPrice(),
                product.getLikeCount().getCount(),
                product.getStock().getQuantity()
        );
    }
}

