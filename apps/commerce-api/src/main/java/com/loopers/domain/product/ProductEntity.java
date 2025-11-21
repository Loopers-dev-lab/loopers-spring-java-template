package com.loopers.domain.product;

import static java.util.Objects.requireNonNull;
import java.math.BigDecimal;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * 상품 엔티티
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_productentity_brand_id", columnList = "brand_id"),
        @Index(name = "idx_productentity_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    @NotNull
    private Price price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    /**
     * 상품 엔티티 생성자
     *
     * @param brandId       브랜드 ID
     * @param name          상품명
     * @param description   상품 설명
     * @param originPrice   정가
     * @param discountPrice 할인가
     * @param stockQuantity 재고 수량
     */
    public ProductEntity(Long brandId, String name, String description,
                         BigDecimal originPrice, BigDecimal discountPrice, Integer stockQuantity) {
        requireNonNull(brandId, "브랜드 ID는 필수입니다.");
        requireNonNull(name, "상품명은 필수입니다.");
        requireNonNull(originPrice, "정가는 필수입니다.");
        requireNonNull(stockQuantity, "재고 수량은 필수입니다.");

        validateName(name);
        validateOriginPrice(originPrice);
        validateDiscountPrice(originPrice, discountPrice);
        validateStockQuantity(stockQuantity);

        this.brandId = brandId;
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.price = discountPrice != null ? Price.of(originPrice, discountPrice) : Price.of(originPrice);
        this.stockQuantity = stockQuantity;
    }

    /**
     * 상품 엔티티를 생성한다.
     *
     * @param request 상품 생성 요청 정보
     * @return 생성된 상품 엔티티
     */
    public static ProductEntity createEntity(ProductDomainCreateRequest request) {
        if (Objects.isNull(request)) {
            throw new IllegalArgumentException("상품 생성 요청 정보는 필수입니다.");
        }

        if (request.brandId() == null) {
            throw new IllegalArgumentException("브랜드 ID는 필수입니다.");
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (request.originPrice() == null || request.originPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정가는 0보다 커야 합니다.");
        }

        if (request.discountPrice() != null && request.discountPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인가는 0보다 커야 합니다.");
        }

        if (request.discountPrice() != null && request.discountPrice().compareTo(request.originPrice()) >= 0) {
            throw new IllegalArgumentException("할인가는 정가보다 작아야 합니다.");
        }

        if (request.stockQuantity() == null || request.stockQuantity() < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }

        return new ProductEntity(
                request.brandId(),
                request.name(),
                request.description(),
                request.originPrice(),
                request.discountPrice(),
                request.stockQuantity()
        );
    }

    /**
     * 재고를 차감한다.
     *
     * @param quantity 차감할 재고 수량
     */
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 재고 수량은 0보다 커야 합니다.");
        }

        if (this.stockQuantity < quantity) {
            throw new CoreException(
                    ErrorType.INSUFFICIENT_STOCK,
                    String.format("재고가 부족합니다. (요청: %d, 보유: %d)", quantity, this.stockQuantity)
            );
        }

        this.stockQuantity -= quantity;
    }

    /**
     * 재고를 원복한다.
     *
     * <p>주문 취소 시 차감된 재고를 다시 복구합니다.</p>
     *
     * @param quantity 원복할 재고 수량
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("원복할 재고 수량은 0보다 커야 합니다.");
        }

        this.stockQuantity += quantity;
    }


    /**
     * 좋아요 수를 증가시킨다.
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수를 감소시킨다.
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 재고 여부를 확인한다.
     *
     * @return 재고가 있는지 여부
     */
    public boolean hasStock() {
        return this.stockQuantity > 0;
    }

    /**
     * 지정된 수량만큼 주문 가능한지 확인한다.
     * TODO : 향후 예약 주문 기능이 추가되면 수정 필요
     *
     * @param quantity 주문 수량
     * @return 주문 가능 여부
     */
    public boolean canOrder(int quantity) {
        return this.stockQuantity >= quantity && quantity > 0;
    }

    /**
     * 할인 여부를 확인한다.
     *
     * @return 할인 상품인지 여부
     */
    public boolean isDiscounted() {
        return this.price.isDiscounted();
    }

    @Override
    protected void guard() {
        if (this.brandId == null) {
            throw new IllegalStateException("브랜드 ID는 필수입니다.");
        }

        if (this.name == null || this.name.isBlank()) {
            throw new IllegalStateException("상품명은 비어있을 수 없습니다.");
        }

        if (this.name.length() > 200) {
            throw new IllegalStateException("상품명은 200자를 초과할 수 없습니다.");
        }

        if (this.price == null) {
            throw new IllegalStateException("가격 정보는 필수입니다.");
        }

        if (this.stockQuantity == null || this.stockQuantity < 0) {
            throw new IllegalStateException("재고 수량은 0 이상이어야 합니다.");
        }

        if (this.likeCount == null || this.likeCount < 0) {
            throw new IllegalStateException("좋아요 수는 0 이상이어야 합니다.");
        }
    }

    /**
     * 상품명 유효성을 검증한다.
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수 입력값입니다.");
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException("상품명은 200자를 초과할 수 없습니다.");
        }
    }

    /**
     * 정가 유효성을 검증한다.
     */
    private void validateOriginPrice(BigDecimal originPrice) {
        if (originPrice == null) {
            throw new IllegalArgumentException("정가는 필수 입력값입니다.");
        }

        if (originPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정가는 0보다 커야 합니다.");
        }
    }

    /**
     * 할인가 유효성을 검증한다.
     */
    private void validateDiscountPrice(BigDecimal originPrice, BigDecimal discountPrice) {
        if (discountPrice == null) {
            return; // 할인가는 선택사항
        }

        if (discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인가는 0보다 커야 합니다.");
        }

        if (discountPrice.compareTo(originPrice) >= 0) {
            throw new IllegalArgumentException("할인가는 정가보다 작아야 합니다.");
        }
    }

    /**
     * 재고 수량 유효성을 검증한다.
     */
    private void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null) {
            throw new IllegalArgumentException("재고 수량은 필수 입력값입니다.");
        }

        if (stockQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getSellingPrice() {
        return price.getSellingPrice();
    }
}
