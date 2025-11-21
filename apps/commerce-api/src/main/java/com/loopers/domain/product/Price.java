package com.loopers.domain.product;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 상품 가격을 표현하는 Value Object
 * 정가와 할인가를 관리하며, 가격 관련 비즈니스 로직을 캡슐화한다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {

    @Column(name = "origin_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal originPrice;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    /**
     * 가격 정보를 생성한다.
     *
     * @param originPrice   정가 (필수)
     * @param discountPrice 할인가 (선택)
     * @return Price 객체
     */
    public static Price of(BigDecimal originPrice, BigDecimal discountPrice) {
        validatePrice(originPrice, discountPrice);

        Price price = new Price();
        price.originPrice = originPrice.setScale(2, RoundingMode.HALF_UP);
        price.discountPrice = discountPrice != null ?
                discountPrice.setScale(2, RoundingMode.HALF_UP) : null;

        return price;
    }


    public static Price of(BigDecimal originPrice) {
        validatePrice(originPrice, null);

        Price price = new Price();
        price.originPrice = originPrice.setScale(2, RoundingMode.HALF_UP);
        price.discountPrice = null;

        return price;
    }

    /**
     * 정가만으로 가격 정보를 생성한다.
     *
     * @param originPrice 정가
     * @return Price 객체
     */
    public static Price ofOriginOnly(BigDecimal originPrice) {
        return of(originPrice, null);
    }

    /**
     * 실제 판매 가격을 반환한다.
     * 할인가가 있으면 할인가를, 없으면 정가를 반환한다.
     *
     * @return 실제 판매 가격
     */
    public BigDecimal getSellingPrice() {
        return discountPrice != null ? discountPrice : originPrice;
    }

    /**
     * 할인율을 계산한다.
     *
     * @return 할인율 (0-100 사이의 값, 할인가가 없으면 0)
     */
    public BigDecimal getDiscountRate() {
        if (discountPrice == null || discountPrice.compareTo(originPrice) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = originPrice.subtract(discountPrice);
        return discount.divide(originPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 할인 여부를 확인한다.
     *
     * @return 할인 상품인지 여부
     */
    public boolean isDiscounted() {
        return discountPrice != null && discountPrice.compareTo(originPrice) < 0;
    }

    /**
     * 할인 금액을 계산한다.
     *
     * @return 할인 금액 (할인가가 없거나 할인가가 정가보다 크거나 같으면 0)
     */
    public BigDecimal getDiscountAmount() {
        if (!isDiscounted()) {
            return BigDecimal.ZERO;
        }
        return originPrice.subtract(discountPrice);
    }

    /**
     * 할인가를 설정한다.
     *
     * @param discountPrice 할인가
     */
    public void applyDiscount(BigDecimal discountPrice) {
        validateDiscountPrice(this.originPrice, discountPrice);
        this.discountPrice = discountPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 할인을 제거한다.
     */
    public void removeDiscount() {
        this.discountPrice = null;
    }

    private static void validatePrice(BigDecimal originPrice, BigDecimal discountPrice) {
        if (originPrice == null) {
            throw new IllegalArgumentException("정가는 필수입니다.");
        }

        if (originPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정가는 0보다 커야 합니다.");
        }

        if (discountPrice != null) {
            validateDiscountPrice(originPrice, discountPrice);
        }
    }

    private static void validateDiscountPrice(BigDecimal originPrice, BigDecimal discountPrice) {
        if (discountPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인가는 음수일 수 없습니다.");
        }

        if (discountPrice.compareTo(originPrice) > 0) {
            throw new IllegalArgumentException("할인가는 정가보다 클 수 없습니다.");
        }
    }
}
