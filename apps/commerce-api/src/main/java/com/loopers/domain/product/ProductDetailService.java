package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import org.springframework.stereotype.Component;

/**
 * 상품 상세 정보 조합 도메인 서비스.
 * <p>
 * 상품 상세 조회 시 Product와 Brand 정보, 좋아요 수를 조합하는 도메인 로직을 처리합니다.
 * 도메인 간 협력 로직(Product + Brand + Like)을 담당합니다.
 * </p>
 * <p>
 * 상태가 없고, 도메인 객체의 협력 중심으로 설계되었습니다.
 * Repository 의존성 없이 도메인 객체(Product, Brand, likesCount)를 파라미터로 받아 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Component
public class ProductDetailService {

    /**
     * 상품과 브랜드 정보, 좋아요 수를 조합하여 상품 상세 정보를 생성합니다.
     *
     * @param product 상품 엔티티
     * @param brand 브랜드 엔티티
     * @param likesCount 좋아요 수
     * @return 조합된 상품 상세 정보
     */
    public ProductDetail combineProductAndBrand(Product product, Brand brand, Long likesCount) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 null일 수 없습니다.");
        }
        if (brand == null) {
            throw new IllegalArgumentException("브랜드 정보는 필수입니다.");
        }
        if (likesCount == null) {
            throw new IllegalArgumentException("좋아요 수는 null일 수 없습니다.");
        }

        return ProductDetail.of(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getBrandId(),
            brand.getName(),
            likesCount
        );
    }
}

