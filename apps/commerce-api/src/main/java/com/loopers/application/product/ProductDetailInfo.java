package com.loopers.application.product;

import com.loopers.domain.product.ProductDetail;

/**
 * packageName : com.loopers.application.product
 * fileName     : ProductDetail
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
public record ProductDetailInfo(
        Long id,
        String name,
        String brandName,
        Long price,
        Long likeCount
) {
    public static ProductDetailInfo from(ProductDetail productDetail) {
        return new ProductDetailInfo(
                productDetail.getId(),
                productDetail.getName(),
                productDetail.getBrandName(),
                productDetail.getPrice(),
                productDetail.getLikeCount()
        );
    }
}
