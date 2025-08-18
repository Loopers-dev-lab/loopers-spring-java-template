package com.loopers.application.like;

import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.product.ProductModel;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductLikeCommand {

    public static class Request {
        public record Toggle(
                Long userId,
                Long productId
        ) {}

        public record Add(
                Long userId,
                Long productId
        ) {}

        public record Remove(
                Long userId,
                Long productId
        ) {}

        public record GetLikedProducts(
                Long userId,
                int page,
                int size
        ) {}
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
            LocalDateTime likedAt
    ) {
        public static LikedProductItem of(ProductLikeModel productLike, ProductModel product, String brandName) {
            return new LikedProductItem(
                    product.getId(),
                    product.getProductName().getValue(),
                    product.getPrice().getValue(),
                    product.getBrandId().getValue(),
                    brandName,
                    product.getImgUrl().getValue(),
                    product.getLikeCount().getValue(),
                    product.getStatus().getValue(),
                    product.getStock().getValue(),
                    productLike.getLikedAt()
            );
        }
    }

    public record LikedProductsData(
            Page<ProductLikeModel> productLikes,
            List<LikedProductItem> likedProductItems
    ) {}

    public record LikeToggleResult(
            boolean isAdded,
            boolean isLiked
    ) {}
}