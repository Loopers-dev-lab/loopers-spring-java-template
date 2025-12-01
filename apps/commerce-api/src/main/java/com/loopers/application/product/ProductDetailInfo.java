package com.loopers.application.product;

import com.loopers.domain.product.ProductDetailProjection;

import java.math.BigDecimal;

public record ProductDetailInfo(
        Long productId,
        String productName,
        Long brandId,
        String brandName,
        BigDecimal priceAmount,
        int likeCount
) {
    public static ProductDetailInfo from(ProductDetailProjection p) {
        return new ProductDetailInfo(
                p.getProductId(),
                p.getProductName(),
                p.getBrandId(),
                p.getBrandName(),
                p.getPrice().getAmount(),
                p.getLikeCount()
        );
    }
}
