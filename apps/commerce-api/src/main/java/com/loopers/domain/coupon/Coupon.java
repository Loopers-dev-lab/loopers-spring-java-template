package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal discountValue;

    @Column(length = 500)
    private String description;

    @Builder
    private Coupon(String name, CouponType type, BigDecimal discountValue, String description) {
        validateName(name);
        validateType(type);
        validateDiscountValue(type, discountValue);

        this.name = name;
        this.type = type;
        this.discountValue = discountValue;
        this.description = description;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 필수입니다.");
        }
    }

    private void validateType(CouponType type) {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }
    }

    private void validateDiscountValue(CouponType type, BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }

        if (type == CouponType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100% 이하여야 합니다.");
            }
        }
    }

    /**
     * 할인 금액을 계산합니다.
     *
     * @param originalAmount 원래 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscountAmount(BigDecimal originalAmount) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "원래 금액은 0보다 커야 합니다.");
        }

        return switch (type) {
            case FIXED_AMOUNT -> calculateFixedAmountDiscount(originalAmount);
            case PERCENTAGE -> calculatePercentageDiscount(originalAmount);
        };
    }

    private BigDecimal calculateFixedAmountDiscount(BigDecimal originalAmount) {
        // 정액 할인은 원래 금액보다 클 수 없음
        if (discountValue.compareTo(originalAmount) > 0) {
            return originalAmount;
        }
        return discountValue;
    }

    private BigDecimal calculatePercentageDiscount(BigDecimal originalAmount) {
        // 정률 할인: (원래 금액 * 할인율 / 100), 소수점 버림
        return originalAmount
            .multiply(discountValue)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
    }
}
