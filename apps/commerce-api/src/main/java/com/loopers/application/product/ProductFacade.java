package com.loopers.application.product;

import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.ProductDetailService;

/**
 * 상품 애플리케이션 파사드
 * - 트랜잭션 경계, DTO 변환, 인증정보 전달 등 애플리케이션 관심사를 담당한다.
 * - 도메인 규칙과 협력은 ProductDetailService에 위임한다.
 */
public class ProductFacade {

    private final ProductDetailService productDetailService;

    public ProductFacade(ProductDetailService productDetailService) {
        this.productDetailService = productDetailService;
    }

    /**
     * 상품 상세 조회
     * @param productId 상품 ID
     * @param userIdOrNull 인증된 사용자 ID (없으면 null)
     */
    public ProductDetailInfo getProductDetail(Long productId, String userIdOrNull) {
        ProductDetail detail = productDetailService.getProductDetail(productId, userIdOrNull);
        return ProductDetailInfo.from(detail);
    }
}
