package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;

import java.math.BigDecimal;

public class ProductV1Dto {
    public record ProductResponse(
            Long productId,
            String productName,
            BigDecimal price,
            int likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.likeCount()
            );
        }
    }
    public record ProductDetailResponse(
            Long productId,
            Long brandId,
            String brandName,
            String productName,
            BigDecimal price,
            int stock,
            int likeCount
    ) {
        public static ProductDetailResponse from(ProductDetailInfo info) {
            return new ProductDetailResponse(
                    info.productId(),
                    info.brandId(),
                    info.brandName(),
                    info.productName(),
                    info.price(),
                    info.stock(),
                    info.likeCount()
            );
        }
    }
}
