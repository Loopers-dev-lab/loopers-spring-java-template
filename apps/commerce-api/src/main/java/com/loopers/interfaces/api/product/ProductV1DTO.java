package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;

import java.math.BigDecimal;

public class ProductV1DTO {

    public record ProductDetailResponse(
            Long id,
            String productCode,
            String productName,
            BigDecimal price,
            int stock,
            Long likeCount,
            BrandResponse brand
    ) {
        public static ProductDetailResponse from(ProductDetailInfo productDetailInfo) {
            return new ProductDetailResponse(
                    productDetailInfo.id(),
                    productDetailInfo.productCode(),
                    productDetailInfo.productName(),
                    productDetailInfo.price(),
                    productDetailInfo.stock(),
                    productDetailInfo.likeCount(),
                    productDetailInfo.brand() != null ?
                            BrandResponse.from(productDetailInfo.brand()) : null
            );
        }
    }

    public record BrandResponse(
            Long id,
            String brandName,
            boolean isActive
    ) {
        public static BrandResponse from(ProductDetailInfo.BrandInfo brandInfo) {
            return new BrandResponse(
                    brandInfo.id(),
                    brandInfo.brandName(),
                    brandInfo.isActive()
            );
        }
    }
}