package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;

/**
 * 상품 상세 도메인 VO
 * - Product, Brand, Like 메타데이터(개수/사용자 좋아요 여부)를 묶는 불변 값 객체
 */
public record ProductDetail(
        Product product,
        Brand brand,
        int likeCount,
        boolean likedByUser
) {
    public static ProductDetail of(Product product, Brand brand, int likeCount, boolean likedByUser) {
        return new ProductDetail(product, brand, likeCount, likedByUser);
    }
}
