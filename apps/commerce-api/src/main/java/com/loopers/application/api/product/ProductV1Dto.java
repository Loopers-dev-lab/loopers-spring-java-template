package com.loopers.application.api.product;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.ProductListItem;
import com.loopers.core.domain.product.ProductListView;

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
            Long likeCount
    ) {

        public static GetProductDetailResponse from(ProductDetail detail) {
            return new GetProductDetailResponse(
                    detail.getProduct().getId().value(),
                    GetProductDetailBrand.from(detail.getBrand()),
                    detail.getProduct().getName().value(),
                    detail.getProduct().getPrice().value(),
                    detail.getProduct().getStock().value(),
                    detail.getProduct().getLikeCount().value()
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
    }
}
