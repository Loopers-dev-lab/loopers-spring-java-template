package com.loopers.application.product;

import java.time.ZonedDateTime;

import com.loopers.domain.product.ProductEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public record ProductInfo(
        Long id,
        String name,
        String description,
        Long likeCount,
        ProductPriceInfo price,
        Long brandId,
        ZonedDateTime createdAt
) {
    /**
     * ProductEntity와 BrandEntity를 조합하여 ProductInfo를 생성한다.
     *
     * @param product 상품 엔티티
     * @return ProductInfo
     */
    public static ProductInfo of(ProductEntity product) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }

        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getLikeCount(),
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                product.getBrandId(),
                product.getCreatedAt()
        );
    }
}
