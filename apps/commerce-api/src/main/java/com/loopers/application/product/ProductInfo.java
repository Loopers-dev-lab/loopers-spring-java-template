package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public record ProductInfo(
        Long id,
        String productCode,
        String productName,
        BigDecimal price,
        Long likeCount,
        BrandInfo brand
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getLikeCount(),
                BrandInfo.from(product.getBrand())
        );
    }
}
