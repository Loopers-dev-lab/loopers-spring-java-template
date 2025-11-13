package com.loopers.domain.product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 도메인 모델 (사용자 관점)
 * - 순수 도메인 객체 (JPA 의존성 없음)
 * - 조회, 재고 확인, 재고 차감 등 사용자 기능 중심
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String imageUrl;
    private Long brandId;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;



    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Product reconstitute(
            Long id,
            String name,
            String description,
            BigDecimal price,
            int stock,
            String imageUrl,
            Long brandId,
            ProductStatus status,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        return new Product(id, name, description, price, stock, imageUrl, brandId, status, createdAt, modifiedAt);
    }

    /**
     * 재고 차감 (주문 시)
     * - 재고는 감소만 가능
     * - 음수 방지는 도메인 레벨에서 처리
     */
    public void decreaseStock(int quantity) {
        if (!isAvailable()) {
            throw new IllegalStateException("판매 가능한 상품이 아닙니다. 상태: " + status);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 양수여야 합니다: " + quantity);
        }
        if (this.stock < quantity) {
            throw new IllegalArgumentException(
                    String.format("재고가 부족합니다. 요청: %d, 현재 재고: %d", quantity, this.stock)
            );
        }

        this.stock -= quantity;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 구매 가능 여부 확인
     * - 정상 상태이고 재고가 있어야 구매 가능
     */
    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE && this.stock > 0;
    }

    /**
     * 조회 가능 여부 확인
     * - 삭제되지 않은 상품만 조회 가능
     */
    public boolean isViewable() {
        return this.status != ProductStatus.DELETED;
    }

    /**
     * 삭제된 상품인지 확인
     */
    public boolean isDeleted() {
        return this.status == ProductStatus.DELETED;
    }

    /**
     * 특정 수량 구매 가능 여부 확인
     */
    public boolean canPurchase(int quantity) {
        return isAvailable() && this.stock >= quantity;
    }

    /**
     * 품절 여부
     */
    public boolean isSoldOut() {
        return this.stock == 0;
    }

    /**
     * 총 구매 금액 계산
     */
    public BigDecimal calculateTotalPrice(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다");
        }
        return this.price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 상품이 특정 가격 범위에 속하는지 확인
     */
    public boolean isPriceInRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return this.price.compareTo(minPrice) >= 0 && this.price.compareTo(maxPrice) <= 0;
    }

    /**
     * 상품이 특정 브랜드에 속하는지 확인
     */
    public boolean isBrandOf(Long brandId) {
        return this.brandId.equals(brandId);
    }


}
