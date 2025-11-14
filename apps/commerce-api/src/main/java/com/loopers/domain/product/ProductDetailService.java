package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

/**
 * 상품 상세 조합 도메인 서비스
 * - Product + Brand + Like 정보를 조합한다.
 * - 상태가 없는 협력 중심 설계.
 */
public class ProductDetailService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final LikeService likeService;

    public ProductDetailService(ProductRepository productRepository,
                                BrandRepository brandRepository,
                                LikeService likeService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.likeService = likeService;
    }

    /**
     * 상품 상세 조회
     * - 상품 존재/조회 가능 여부 확인
     * - 브랜드 존재 확인
     * - 좋아요 수 및 사용자 좋아요 여부 계산
     */
    public ProductDetail getProductDetail(Long productId, String userIdOrNull) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: " + productId));

        if (!product.isViewable()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "조회할 수 없는 상품입니다");
        }

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: " + product.getBrandId()));

        int likeCount = likeService.getLikeCount(productId);
        boolean likedByUser = userIdOrNull != null && likeService.isLiked(userIdOrNull, productId);

        return ProductDetail.of(product, brand, likeCount, likedByUser);
    }
}
