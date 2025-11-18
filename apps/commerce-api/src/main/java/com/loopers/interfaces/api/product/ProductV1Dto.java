package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductSummaryInfo;
import com.loopers.domain.product.enums.ProductSortCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductV1Dto {

    public record ProductSummaryResponse(
            Long id,
            String name,
            String brandName,
            BigDecimal price,
            int likeCount,
            boolean isLikedByMember
    ) {
        public static ProductSummaryResponse from(ProductSummaryInfo info) {
            return new ProductSummaryResponse(
                    info.getId(),
                    info.getName(),
                    info.getBrandName(),
                    info.getPrice().getAmount(),
                    info.getLikeCount(),
                    info.isLikedByMember()
            );
        }
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            String description,
            String brandName,
            String brandDescription,
            BigDecimal price,
            int stockQuantity,
            int likeCount,
            boolean isLikedByMember
    ) {
        public static ProductDetailResponse from(ProductDetailInfo info) {
            return new ProductDetailResponse(
                    info.getId(),
                    info.getName(),
                    info.getDescription(),
                    info.getBrandName(),
                    info.getBrandDescription(),
                    info.getPrice().getAmount(),
                    info.getStock().getQuantity(),
                    info.getLikeCount(),
                    info.isLikedByMember()
            );
        }
    }

    public record ProductSearchRequest(
            String keyword,
            ProductSortCondition sort,
            @NotNull @Min(0) Integer page,
            @NotNull @Min(1) Integer size
    ) {}
}