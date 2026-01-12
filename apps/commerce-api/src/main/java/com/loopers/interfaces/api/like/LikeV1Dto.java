package com.loopers.interfaces.api.like;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.ProductModel;
import java.util.List;
import java.util.stream.Collectors;

public class LikeV1Dto {
    public record LikeResponse(boolean liked) {
        public static LikeResponse from(boolean liked) {
            return new LikeResponse(liked);
        }
    }

    public record LikedProductsResponse(List<ProductInfo> products) {
        public static LikedProductsResponse from(List<ProductModel> products) {
            List<ProductInfo> productInfos = products.stream()
                .map(product -> new ProductInfo(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getPrice(),
                    product.getLikeCount()
                ))
                .collect(Collectors.toList());
            return new LikedProductsResponse(productInfos);
        }
    }

    public record LikeCountResponse(Long count) {
        public static LikeCountResponse from(long count) {
            return new LikeCountResponse(count);
        }
    }

    public record ToggleLikeRequest(Long productId) {}
}

