package com.loopers.interfaces.api.like;

import com.loopers.domain.product.Product;

import java.util.List;

public class ProductLikeDto {

    public record LikeResponse(
            boolean liked,
            long totalLikes
    ) {
        public static LikeResponse from(boolean liked, long totalLikes) {
            return new LikeResponse(liked, totalLikes);
        }
    }

    public record LikedProductsResponse(
            List<ProductSummary> products,
            int totalCount
    ) {
        public static LikedProductsResponse from(List<Product> products) {
            List<ProductSummary> summaries = products.stream()
                    .map(ProductSummary::from)
                    .toList();

            return new LikedProductsResponse(summaries, summaries.size());
        }
    }

    public record ProductSummary(
            Long id,
            String name,
            String description,
            int price,
            Long stock,
            Long totalLikes
    ) {
        public static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock(),
                    product.getTotalLikes()
            );
        }
    }
}
