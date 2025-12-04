package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductMaterializedViewEntity;

/**
 * 상품 상세 정보 DTO
 *
 * MV 테이블 우선 사용 (성능 최적화)
 * isLiked: 비로그인 false, 로그인 사용자의 좋아요 여부
 */
public record ProductDetailInfo(
        Long id,
        String name,
        String description,
        Long likeCount,
        Integer stockQuantity,
        ProductPriceInfo price,
        BrandInfo brand,
        Boolean isLiked  // 사용자 좋아요 여부
) {

    /**
     * MV 엔티티와 좋아요 여부로 생성 (권장)
     */
    public static ProductDetailInfo from(ProductMaterializedViewEntity mv, Boolean isLiked) {
        if (mv == null) {
            throw new IllegalArgumentException("MV 엔티티는 필수입니다.");
        }

        return new ProductDetailInfo(
                mv.getProductId(),
                mv.getName(),
                mv.getDescription(),
                mv.getLikeCount(),
                mv.getStockQuantity(),
                new ProductPriceInfo(
                        mv.getPrice().getOriginPrice(),
                        mv.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        mv.getBrandId(),
                        mv.getBrandName()
                ),
                isLiked
        );
    }

    /**
     * ProductEntity + BrandEntity + 좋아요수로 생성 (MV 사용 권장)
     */
    public static ProductDetailInfo of(ProductEntity product, BrandEntity brand, Long likeCount, Boolean isLiked) {
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }

        if (brand == null) {
            throw new IllegalArgumentException("브랜드 정보는 필수입니다.");
        }

        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getDescription(),
                likeCount != null ? likeCount : 0L,
                product.getStockQuantity(),
                new ProductPriceInfo(
                        product.getPrice().getOriginPrice(),
                        product.getPrice().getDiscountPrice()
                ),
                new BrandInfo(
                        brand.getId(),
                        brand.getName()
                ),
                isLiked
        );
    }

    public static ProductDetailInfo fromWithSyncLike(ProductDetailInfo productDetailInfo, Boolean isLiked) {
        return new ProductDetailInfo(
                productDetailInfo.id(),
                productDetailInfo.name(),
                productDetailInfo.description(),
                productDetailInfo.likeCount(),
                productDetailInfo.stockQuantity(),
                productDetailInfo.price(),
                productDetailInfo.brand(),
                isLiked
        );
    }
}
