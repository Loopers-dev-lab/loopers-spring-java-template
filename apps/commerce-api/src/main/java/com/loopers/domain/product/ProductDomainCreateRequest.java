package com.loopers.domain.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

/**
 * 상품 도메인 생성 요청 DTO
 * <p>
 * 도메인 레이어에서 사용하는 상품 생성 요청 정보입니다.
 * Application Layer에서 Domain Layer로 전달되는 데이터 구조입니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
public record ProductDomainCreateRequest(
        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 200, message = "상품명은 200자를 초과할 수 없습니다.")
        String name,

        String description,

        @NotNull(message = "정가는 필수입니다.")
        @Min(value = 1, message = "정가는 0보다 커야 합니다.")
        BigDecimal originPrice,

        @PositiveOrZero(message = "할인가는 0 이상이어야 합니다.")
        BigDecimal discountPrice,

        @NotNull(message = "재고 수량은 필수입니다.")
        @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
        Integer stockQuantity
) {
    /**
     * 정적 팩토리 메서드 (할인가 없는 경우)
     *
     * @param brandId       브랜드 ID
     * @param name          상품명
     * @param description   상품 설명
     * @param originPrice   정가
     * @param stockQuantity 재고 수량
     * @return ProductDomainCreateRequest 인스턴스
     */
    public static ProductDomainCreateRequest of(Long brandId, String name, String description,
                                                BigDecimal originPrice, Integer stockQuantity) {
        return new ProductDomainCreateRequest(brandId, name, description, originPrice, null, stockQuantity);
    }

    /**
     * 정적 팩토리 메서드 (할인가 있는 경우)
     *
     * @param brandId       브랜드 ID
     * @param name          상품명
     * @param description   상품 설명
     * @param originPrice   정가
     * @param discountPrice 할인가
     * @param stockQuantity 재고 수량
     * @return ProductDomainCreateRequest 인스턴스
     */
    public static ProductDomainCreateRequest of(Long brandId, String name, String description,
                                                BigDecimal originPrice, BigDecimal discountPrice,
                                                Integer stockQuantity) {
        return new ProductDomainCreateRequest(brandId, name, description, originPrice, discountPrice, stockQuantity);
    }
}

