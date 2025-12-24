package com.loopers.application.api.product;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.product.*;
import com.loopers.core.domain.product.ProductRankingList.ProductRankingItem;

import java.math.BigDecimal;
import java.util.List;

public class ProductV1Dto {

    public record GetProductListResponse(
            List<GetProductListItemResponse> items,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static GetProductListResponse from(ProductListView view) {
            return new GetProductListResponse(
                    view.getItems().stream()
                            .map(GetProductListItemResponse::from)
                            .toList(),
                    view.getTotalElements(),
                    view.getTotalPages(),
                    view.isHasNext(),
                    view.isHasPrevious()
            );
        }

        public record GetProductListItemResponse(
                String productId,
                String brandId,
                String name,
                BigDecimal price,
                Long stock,
                Long likeCount
        ) {
            public static GetProductListItemResponse from(ProductListItem item) {
                return new GetProductListItemResponse(
                        item.getProductId().value(),
                        item.getBrandId().value(),
                        item.getName().value(),
                        item.getPrice().value(),
                        item.getStock().value(),
                        item.getLikeCount().value()
                );
            }
        }
    }

    public record GetProductResponse(
            String productId,
            String brandId,
            String name,
            BigDecimal price,
            Long stock,
            Long likeCount
    ) {
        public static GetProductResponse from(Product product) {
            return new GetProductResponse(
                    product.getId().value(),
                    product.getBrandId().value(),
                    product.getName().value(),
                    product.getPrice().value(),
                    product.getStock().value(),
                    product.getLikeCount().value()
            );
        }
    }

    public record GetProductDetailResponse(
            String id,
            GetProductDetailBrand brand,
            String name,
            BigDecimal price,
            Long stock,
            Long likeCount,
            Long ranking,
            Double score
    ) {

        public static GetProductDetailResponse from(ProductDetail detail) {
            return new GetProductDetailResponse(
                    detail.getProduct().getId().value(),
                    GetProductDetailBrand.from(detail.getBrand()),
                    detail.getProduct().getName().value(),
                    detail.getProduct().getPrice().value(),
                    detail.getProduct().getStock().value(),
                    detail.getProduct().getLikeCount().value(),
                    detail.getRanking().ranking(),
                    detail.getRanking().score()
            );
        }

        public record GetProductDetailBrand(
                String id,
                String name,
                String description
        ) {

            public static GetProductDetailBrand from(Brand brand) {
                return new GetProductDetailBrand(
                        brand.getId().value(),
                        brand.getName().value(),
                        brand.getDescription().value()
                );
            }
        }

        public record GetProductRankingsResponse(
                List<ProductRankingResponse> products,
                long totalElements,
                int totalPages,
                boolean hasNext,
                boolean hasPrevious
        ) {

            public static GetProductRankingsResponse from(ProductRankingList list) {
                return new GetProductRankingsResponse(
                        list.products().stream()
                                .map(ProductRankingResponse::from)
                                .toList(),
                        list.totalElements(),
                        list.totalPages(),
                        list.hasNext(),
                        list.hasPrevious()
                );
            }

            public record ProductRankingResponse(
                    String id,
                    Long ranking,
                    String brandName,
                    String name,
                    BigDecimal price,
                    Long likeCount,
                    Double score
            ) {
                public static ProductRankingResponse from(ProductRankingItem item) {
                    return new ProductRankingResponse(
                            item.id().value(),
                            item.ranking(),
                            item.brandName().value(),
                            item.name().value(),
                            item.price().value(),
                            item.likeCount().value(),
                            item.score()
                    );
                }
            }
        }
    }
}
