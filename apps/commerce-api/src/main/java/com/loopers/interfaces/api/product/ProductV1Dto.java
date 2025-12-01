package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public class ProductV1Dto {
    public record ProductResponse(
            Long productId,
            String productName,
            Long brandId,
            BigDecimal price,
            int likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                    info.id(),
                    info.name(),
                    info.brandId(),
                    info.priceAmount(),
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
            int likeCount
    ) {
        public static ProductDetailResponse from(ProductDetailInfo info) {
            return new ProductDetailResponse(
                    info.productId(),
                    info.brandId(),
                    info.brandName(),
                    info.productName(),
                    info.priceAmount(),
                    info.likeCount()
            );
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }
}
