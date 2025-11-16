package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.Product;
import java.math.BigDecimal;

public record ProductInfo(
    Long id,
    String name,
    BigDecimal price,
    Integer stock,
    String description,
    BrandInfo brand,
    Long likeCount
) {
    public static ProductInfo from(Product product, Long likeCount) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getDescription(),
            BrandInfo.from(product.getBrand()),
            likeCount
        );
    }

    public static ProductInfo from(Product product) {
        return from(product, 0L);
    }
}
