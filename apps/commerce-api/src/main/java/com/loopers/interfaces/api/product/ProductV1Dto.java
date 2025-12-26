package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductListInfo;

import java.util.List;

public class ProductV1Dto {

    public record ProductListResponse(
            List<ProductItemResponse> products,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static ProductListResponse from(ProductListInfo info) {
            List<ProductItemResponse> products = info.contents().stream()
                    .map(ProductItemResponse::from)
                    .toList();

            return new ProductListResponse(
                    products,
                    info.page(),
                    info.size(),
                    info.totalElements(),
                    info.totalPages()
            );
        }
    }

    public record ProductItemResponse(
            Long id,
            String name,
            Long price,
            Long brandId,
            String brandName,
            Long likeCount
    ) {
        public static ProductItemResponse from(ProductListInfo.ProductContent content) {
            return new ProductItemResponse(
                    content.id(),
                    content.name(),
                    content.price(),
                    content.brandId(),
                    content.brandName(),
                    content.likeCount()
            );
        }
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            Long price,
            Integer stock,
            Long brandId,
            String brandName,
            Long likeCount,
            Long rank
    ) {
        public static ProductDetailResponse from(ProductDetailInfo info) {
            return new ProductDetailResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.stock(),
                    info.brandId(),
                    info.brandName(),
                    info.likeCount(),
                    info.rank()
            );
        }
    }
}
