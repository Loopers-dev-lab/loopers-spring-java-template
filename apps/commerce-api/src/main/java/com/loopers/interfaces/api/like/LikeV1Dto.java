package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;
import com.loopers.application.product.ProductInfo;
import java.util.List;

public class LikeV1Dto {

    public record LikeResponse(
            Long productId,
            Integer likeCount
    ) {
        public static LikeResponse from(LikeInfo info) {
            return new LikeResponse(
                    info.productId(),
                    info.likeCount()
            );
        }
    }

    public record LikedProductListResponse(
            List<ProductItem> products
    ) {
        public static LikedProductListResponse from(List<ProductInfo> productInfos) {
            List<ProductItem> items = productInfos.stream()
                    .map(ProductItem::from)
                    .toList();
            return new LikedProductListResponse(items);
        }
    }

    public record ProductItem(
            Long id,
            String name,
            Long brandId,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer stock
    ) {
        public static ProductItem from(ProductInfo info) {
            return new ProductItem(
                    info.id(),
                    info.name(),
                    info.brandId(),
                    info.brandName(),
                    info.price(),
                    info.likeCount(),
                    info.stock()
            );
        }
    }
}

