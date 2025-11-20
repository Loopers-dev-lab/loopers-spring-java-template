package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

public record ProductWithBrandInfo(Long id, String name, Integer price, long stock, long likeCount, Long brandId, String brandName) {
    public static ProductWithBrandInfo from(final Product product, final Brand brand) {
        return new ProductWithBrandInfo(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock().getQuantity(),
                product.getLikeCount().getValue(),
                brand.getId(),
                brand.getName()
        );
    }
}
