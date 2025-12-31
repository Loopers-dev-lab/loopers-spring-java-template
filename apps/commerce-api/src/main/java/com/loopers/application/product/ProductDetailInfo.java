package com.loopers.application.product;

import com.loopers.domain.product.ProductDetailProjection;

import java.math.BigDecimal;

public record ProductDetailInfo(
        Long productId,
        String productName,
        Long brandId,
        String brandName,
        BigDecimal priceAmount,
        int likeCount,
        Long rank
) {
    public static ProductDetailInfo of(ProductDetailProjection p, Long rank) {
        return new ProductDetailInfo(
                p.getProductId(),
                p.getProductName(),
                p.getBrandId(),
                p.getBrandName(),
                p.getPrice().getAmount(),
                p.getLikeCount(),
                rank
        );
    }
}
