package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.discount.CouponDiscountStrategy;
import com.loopers.domain.coupon.discount.CouponDiscountStrategyFactory;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 도메인 엔티티.
 * <p>
 * 쿠폰의 기본 정보(코드, 타입, 할인 금액/비율)를 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseEntity {
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    /**
     * Create a Coupon instance with validated code, type, and discount value.
     *
     * @param code coupon code; required, non-blank, maximum 50 characters
     * @param type coupon type; required
     * @param discountValue discount value; required and greater than 0 — for FIXED_AMOUNT the amount, for PERCENTAGE the percentage (1–100)
     * @throws CoreException if validation fails
     */
    public Coupon(String code, CouponType type, Integer discountValue) {
        validateCode(code);
        validateType(type);
        validateDiscountValue(type, discountValue);
        this.code = code;
        this.type = type;
        this.discountValue = discountValue;
    }

    /**
     * Create a Coupon instance with the given code, type, and discount value.
     *
     * @param code         the coupon code (non-blank, max 50 characters)
     * @param type         the coupon type (must not be null)
     * @param discountValue the discount value (must be greater than 0; if type is PERCENTAGE, must be ≤ 100)
     * @return the created Coupon instance
     * @throws CoreException if any input validation fails
     */
    public static Coupon of(String code, CouponType type, Integer discountValue) {
        return new Coupon(code, type, discountValue);
    }

    /**
     * Validates the coupon code.
     *
     * @param code the coupon code to validate
     * @throws CoreException if `code` is null, blank, or longer than 50 characters
     */
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 코드는 필수입니다.");
        }
        if (code.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 코드는 50자를 초과할 수 없습니다.");
        }
    }

    /**
     * Validates that a coupon type is provided.
     *
     * @param type the coupon type to validate
     * @throws CoreException if {@code type} is {@code null}; error type set to {@code ErrorType.BAD_REQUEST}
     */
    private void validateType(CouponType type) {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }
    }

    /**
     * Validates the discount value for the specified coupon type.
     *
     * @param type the coupon type to validate against
     * @param discountValue the discount value to validate
     * @throws CoreException if `discountValue` is null, less than or equal to 0, or if `type` is `PERCENTAGE` and `discountValue` is greater than 100
     */
    private void validateDiscountValue(CouponType type, Integer discountValue) {
        if (discountValue == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 필수입니다.");
        }
        if (discountValue <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
        if (type == CouponType.PERCENTAGE && discountValue > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 쿠폰의 할인 비율은 100을 초과할 수 없습니다.");
        }
    }

    /**
     * Calculate the discount amount for a given order amount using this coupon.
     *
     * @param orderAmount     the order total; must be greater than 0
     * @param strategyFactory factory used to obtain the discount strategy for this coupon's type
     * @return the calculated discount amount
     * @throws CoreException if orderAmount is null or less than or equal to 0
     */
    public Integer calculateDiscountAmount(Integer orderAmount, CouponDiscountStrategyFactory strategyFactory) {
        if (orderAmount == null || orderAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 금액은 0보다 커야 합니다.");
        }

        // 전략 패턴을 사용하여 쿠폰 타입별 할인 계산
        CouponDiscountStrategy strategy = strategyFactory.getStrategy(this.type);
        return strategy.calculateDiscountAmount(orderAmount, this.discountValue);
    }
}
