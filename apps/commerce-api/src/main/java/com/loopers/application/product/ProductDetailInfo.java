package com.loopers.application.product;

import com.loopers.domain.product.ProductDetail;

/**
 * 상품 상세 Application DTO
 * - Controller/API 계층으로 전달되는 응답 모델
 */
public record ProductDetailInfo(
        Long productId,
        String productName,
        String description,
        String imageUrl,
        Long brandId,
        String brandName,
        int likeCount,
        boolean likedByUser
) {
    public static ProductDetailInfo from(ProductDetail d) {
        return new ProductDetailInfo(
                d.product().getId(),
                d.product().getName(),
                d.product().getDescription(),
                d.product().getImageUrl(),
                d.brand().getId(),
                d.brand().getName(),
                d.likeCount(),
                d.likedByUser()
        );
    }
}
