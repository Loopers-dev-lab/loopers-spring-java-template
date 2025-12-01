package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductListProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductInfo(
        Long id,
        String name,
        BigDecimal priceAmount,
        Long brandId,
        int likeCount,
        LocalDateTime createdAt
) {
    public static ProductInfo from(ProductListProjection p) {
        return new ProductInfo(
                p.getId(),
                p.getName(),
                p.getPrice().getAmount(),
                p.getBrandId(),
                p.getLikeCount(),
                p.getCreatedAt().toLocalDateTime()
        );
    }
    public static ProductInfo from(Product p, ProductLikeCount plc) {
        return new ProductInfo(
                p.getId(),
                p.getName(),
                p.getPrice().getAmount(),
                p.getBrandId(),
                plc.getLikeCount(),
                p.getCreatedAt().toLocalDateTime()
        );
    }
}
