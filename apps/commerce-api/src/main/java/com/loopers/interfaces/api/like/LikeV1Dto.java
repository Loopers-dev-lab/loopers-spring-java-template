package com.loopers.interfaces.api.like;

import com.loopers.application.product.ProductDetailInfo;

import java.util.List;

public class LikeV1Dto {
    public record LikedProductResponse(
            Long productId,
            String productName,
            Long price,
            String brandName,
            Long likeCount
    ) {
        public static LikedProductResponse from(ProductDetailInfo info) {
            return new LikedProductResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.brandName(),
                    info.likeCount()
            );
        }
    }

    public record LikedProductListResponse(
            List<LikedProductResponse> products,
            int count
    ) {
        public static LikedProductListResponse of(List<ProductDetailInfo> productInfos) {
            List<LikedProductResponse> list = productInfos.stream()
                    .map(LikedProductResponse::from)
                    .toList();
            return new LikedProductListResponse(list, list.size());
        }
    }
}
