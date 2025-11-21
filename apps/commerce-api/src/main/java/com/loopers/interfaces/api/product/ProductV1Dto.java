package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductInfo;

import java.math.BigDecimal;
import java.util.List;

public class ProductV1Dto {
    public record ProductResponse(
        Long id,
        String name,
        Integer stockQuantity,
        BigDecimal priceAmount,
        String brandName,
        Long likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.getId(),
                info.getName(),
                info.getStockQuantity(),
                info.getPriceAmount(),
                info.getBrandName(),
                info.getLikeCount()
            );
        }
    }

    public record ProductListItem(
        Long id,
        String name,
        BigDecimal priceAmount,
        Integer stockQuantity,
        Long likeCount
    ) {}

    public record ProductListResponse(
        List<ProductListItem> items
    ) {}
}
