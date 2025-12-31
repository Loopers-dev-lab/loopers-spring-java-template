package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductInfo;

import java.math.BigDecimal;

public class ProductV1Dto {
    public record ProductResponse(
        Long id,
        String name,
        Integer stockQuantity,
        BigDecimal priceAmount,
        String brandName,
        Long likeCount,
        Integer rank
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.getId(),
                info.getName(),
                info.getStockQuantity(),
                info.getPriceAmount(),
                info.getBrandName(),
                info.getLikeCount(),
                null
            );
        }
        public static ProductResponse from(ProductInfo info, Integer rank) {
            return new ProductResponse(
                info.getId(),
                info.getName(),
                info.getStockQuantity(),
                info.getPriceAmount(),
                info.getBrandName(),
                info.getLikeCount(),
                rank
            );
        }
    }

    public record ProductListItem(
            Long id,
            String name,
            BigDecimal priceAmount,
            String brandName,
            Integer stockQuantity,
            Long likeCount
    ) {}
}
