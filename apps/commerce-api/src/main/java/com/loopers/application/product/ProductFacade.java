package com.loopers.application.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;
    private final LikeService likeService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        Page<ProductEntity> products = productService.getProducts(productSearchFilter);

        // 각 상품의 브랜드 정보 조회
        return products.map(ProductInfo::of);
    }


    /**
     * 상품 상세 정보를 좋아요 정보와 함께 조회합니다.
     *
     * @param productId 상품 ID
     * @param username  사용자명 (null인 경우 비로그인 사용자)
     * @return 상품 상세 정보 (좋아요 여부 포함)
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String username) {
        // 1. Product 조회
        ProductEntity product = productService.getProductDetail(productId);

        // 2. Brand 조회
        BrandEntity brand = brandService.getBrandById(product.getBrandId());

        // 3. 사용자의 좋아요 여부 확인
        Boolean isLiked = username != null
                ? likeService.findLike(userService.getUserByUsername(username).getId(), productId)
                .map(like -> like.getDeletedAt() == null)
                .orElse(false)
                : false;
        // 4. DTO 조합 후 반환
        return ProductDetailInfo.of(product, brand, isLiked);
    }
}
