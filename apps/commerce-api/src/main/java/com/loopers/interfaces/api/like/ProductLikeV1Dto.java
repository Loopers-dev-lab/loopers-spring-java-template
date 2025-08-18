package com.loopers.interfaces.api.like;

import com.loopers.application.like.ProductLikeCommand;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductLikeV1Dto {

    public record LikeRequest(
            Long productId
    ) {}

    public record LikeResponse(
            Long productId,
            boolean isLiked,
            String message
    ) {
        public static LikeResponse success(Long productId, boolean isLiked) {
            String message = isLiked ? "상품 좋아요를 추가했습니다." : "상품 좋아요를 취소했습니다.";
            return new LikeResponse(productId, isLiked, message);
        }
    }

    public record LikedProductsResponse(
            List<LikedProductItem> items,
            long totalCount,
            int page,
            int size
    ) {
        public static LikedProductsResponse of(List<ProductLikeCommand.LikedProductItem> likedProductItems, long totalCount, int page, int size) {
            List<LikedProductItem> items = likedProductItems.stream()
                    .map(LikedProductItem::from)
                    .toList();
            return new LikedProductsResponse(items, totalCount, page, size);
        }
    }

    public record LikedProductItem(
            Long productId,
            String name,
            BigDecimal price,
            Long brandId,
            String brandName,
            String imgUrl,
            BigDecimal likeCount,
            String status,
            BigDecimal stock,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime likedAt
    ) {
        public static LikedProductItem from(ProductLikeCommand.LikedProductItem item) {
            return new LikedProductItem(
                    item.productId(),
                    item.name(),
                    item.price(),
                    item.brandId(),
                    item.brandName(),
                    item.imgUrl(),
                    item.likeCount(),
                    item.status(),
                    item.stock(),
                    item.likedAt()
            );
        }
    }
}