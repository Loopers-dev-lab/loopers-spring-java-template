package com.loopers.application.api.productlike;

import com.loopers.core.domain.productlike.LikeProductListItem;
import com.loopers.core.domain.productlike.LikeProductListView;

import java.math.BigDecimal;
import java.util.List;

public class ProductLikeV1Dto {

    public record LikeProductsResponse(
            List<LikeProductResponse> items,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static LikeProductsResponse from(LikeProductListView view) {
            return new LikeProductsResponse(
                    view.getItems().stream()
                            .map(LikeProductResponse::from)
                            .toList(),
                    view.getTotalElements(),
                    view.getTotalPages(),
                    view.isHasNext(),
                    view.isHasPrevious()
            );
        }


        public record LikeProductResponse(
                String productId,
                String brandId,
                String name,
                BigDecimal price,
                Long stock,
                Long likeCount
        ) {
            public static LikeProductResponse from(LikeProductListItem item) {
                return new LikeProductResponse(
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
}
