package com.loopers.domain.product;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Product 엔티티에 대한 동적 쿼리 생성을 위한 Specification 빌더
 */
public class ProductSpecification {

    /**
     * 삭제되지 않은 상품만 조회
     */
    public static Specification<Product> isNotDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    /**
     * 상품명에 키워드가 포함된 상품 조회 (LIKE 검색)
     */
    public static Specification<Product> hasProductName(String productName) {
        return (root, query, criteriaBuilder) -> {
            if (productName == null || productName.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(root.get("productName"), "%" + productName.trim() + "%");
        };
    }

    /**
     * 특정 브랜드의 상품만 조회
     */
    public static Specification<Product> hasBrandId(Long brandId) {
        return (root, query, criteriaBuilder) -> {
            if (brandId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("brand").get("id"), brandId);
        };
    }

}
