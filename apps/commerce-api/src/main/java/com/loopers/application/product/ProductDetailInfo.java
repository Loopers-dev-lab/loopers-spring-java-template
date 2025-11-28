package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductDetailInfo(
        Long id,
        String productCode,
        String productName,
        BigDecimal price,
        int stock,
        Long likeCount,
        BrandInfo brand
) {
    public static ProductDetailInfo from(Product product) {
        return new ProductDetailInfo(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getStockQuantity(),
                product.getLikeCount(),
                product.getBrand() != null ? BrandInfo.from(product.getBrand()) : null
        );
    }

    public record BrandInfo(
            Long id,
            String brandName,
            boolean isActive
    ) {
        public static BrandInfo from(Brand brand) {
            return new BrandInfo(
                    brand.getId(),
                    brand.getBrandName(),
                    brand.isActive()
            );
        }
    }
}