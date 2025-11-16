package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 도메인 엔티티.
 * <p>
 * 상품의 기본 정보(이름, 가격, 재고, 브랜드)를 관리하며,
 * 주문 시 재고 차감 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "ref_brand_id", nullable = false)
    private Long brandId;

    /**
     * Product 인스턴스를 생성합니다.
     *
     * @param name 상품 이름 (필수)
     * @param price 상품 가격 (필수, 0 이상)
     * @param stock 상품 재고 (필수, 0 이상)
     * @param brandId 브랜드 ID (필수)
     * @throws CoreException 유효성 검증 실패 시
     */
    public Product(String name, Integer price, Integer stock, Long brandId) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);
        validateBrandId(brandId);
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.brandId = brandId;
    }

    /**
     * Product 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param name 상품 이름
     * @param price 상품 가격
     * @param stock 상품 재고
     * @param brandId 브랜드 ID
     * @return 생성된 Product 인스턴스
     * @throws CoreException 유효성 검증 실패 시
     */
    public static Product of(String name, Integer price, Integer stock, Long brandId) {
        return new Product(name, price, stock, brandId);
    }

    /**
     * 재고를 차감합니다.
     * 재고는 감소만 가능하며 음수가 되지 않도록 도메인 레벨에서 검증합니다.
     *
     * @param quantity 차감할 수량 (0보다 커야 함)
     * @throws CoreException quantity가 null, 0 이하이거나 재고가 부족할 경우
     */
    public void decreaseStock(Integer quantity) {
        validateQuantity(quantity);
        if (this.stock < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, 
                String.format("재고가 부족합니다. (현재 재고: %d, 요청 수량: %d)", this.stock, quantity));
        }
        this.stock -= quantity;
    }

    /**
     * 재고를 증가시킵니다.
     * 주문 취소 시 재고를 원복하는 데 사용됩니다.
     *
     * @param quantity 증가시킬 수량 (0보다 커야 함)
     * @throws CoreException quantity가 null이거나 0 이하일 경우
     */
    public void increaseStock(Integer quantity) {
        validateQuantity(quantity);
        this.stock += quantity;
    }

    /**
     * 상품 이름의 유효성을 검증합니다.
     *
     * @param name 검증할 상품 이름
     * @throws CoreException name이 null이거나 공백일 경우
     */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 이름은 필수입니다.");
        }
    }

    /**
     * 상품 가격의 유효성을 검증합니다.
     *
     * @param price 검증할 상품 가격
     * @throws CoreException price가 null이거나 0 미만일 경우
     */
    private void validatePrice(Integer price) {
        if (price == null || price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다.");
        }
    }

    /**
     * 상품 재고의 유효성을 검증합니다.
     *
     * @param stock 검증할 상품 재고
     * @throws CoreException stock이 null이거나 0 미만일 경우
     */
    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 재고는 0 이상이어야 합니다.");
        }
    }

    /**
     * 브랜드 ID의 유효성을 검증합니다.
     *
     * @param brandId 검증할 브랜드 ID
     * @throws CoreException brandId가 null일 경우
     */
    private void validateBrandId(Long brandId) {
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
    }

    /**
     * 수량의 유효성을 검증합니다.
     *
     * @param quantity 검증할 수량
     * @throws CoreException quantity가 null이거나 0 이하일 경우
     */
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }
    }
}

