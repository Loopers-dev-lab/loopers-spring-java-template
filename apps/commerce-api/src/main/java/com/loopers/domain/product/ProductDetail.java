package com.loopers.domain.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 상품 상세 정보 Value Object.
 * <p>
 * 상품 상세 조회 시 Product, Brand 정보, 좋아요 수를 조합한 결과를 나타냅니다.
 * 값으로 식별되며 불변성을 가집니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Getter
@EqualsAndHashCode
public class ProductDetail {
    private final Long id;
    private final String name;
    private final Integer price;
    private final Integer stock;
    private final Long brandId;
    private final String brandName;
    private final Long likesCount;

    private ProductDetail(Long id, String name, Integer price, Integer stock, Long brandId, String brandName, Long likesCount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.brandId = brandId;
        this.brandName = brandName;
        this.likesCount = likesCount;
    }

    /**
     * ProductDetail 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param id 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param stock 상품 재고
     * @param brandId 브랜드 ID
     * @param brandName 브랜드 이름
     * @param likesCount 좋아요 수
     * @return 생성된 ProductDetail 인스턴스
     */
    public static ProductDetail of(Long id, String name, Integer price, Integer stock, Long brandId, String brandName, Long likesCount) {
        return new ProductDetail(id, name, price, stock, brandId, brandName, likesCount);
    }
}

