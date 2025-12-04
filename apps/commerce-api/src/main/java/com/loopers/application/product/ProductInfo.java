package com.loopers.application.product;

import java.time.ZonedDateTime;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMaterializedViewEntity;

/**
 * 상품 목록 정보 DTO
 *
 * MV 테이블 우선 사용 (성능 최적화)
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
     * MV 엔티티로 생성 (권장)
     */
    public static ProductInfo from(ProductMaterializedViewEntity mv) {
        if (mv == null) {
            throw new IllegalArgumentException("MV 엔티티는 필수입니다.");
        }

        return new ProductInfo(
                mv.getProductId(),
                mv.getName(),
                mv.getDescription(),
                mv.getLikeCount(),
                new ProductPriceInfo(
                        mv.getPrice().getOriginPrice(),
                        mv.getPrice().getDiscountPrice()
                ),
                mv.getBrandId(),
                mv.getCreatedAt()
        );
    }

    /**
     * ProductEntity + 좋아요수로 생성 (레거시, MV 사용 권장)
     */
    public static ProductInfo of(ProductEntity product, Long likeCount) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }

        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                likeCount != null ? likeCount : 0L,
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                product.getBrandId(),
                product.getCreatedAt()
        );
    }

    /**
     * ProductEntity로 생성 (레거시, MV 사용 권장)
     */
    public static ProductInfo of(ProductEntity product) {
        return of(product, 0L);
    }
}
