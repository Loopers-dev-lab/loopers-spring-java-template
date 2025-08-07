package com.loopers.domain.coupon.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * 쿠폰 할인 값
 * - FIXED 타입: 고정 할인 금액 (예: 5000원)
 * - RATE 타입: 할인률 (예: 10% -> 0.1)
 */
@Embeddable
public class CouponValue {
    
    @Column(name = "coupon_value", nullable = false)
    private BigDecimal value;

    public CouponValue() {
    }

    private CouponValue(BigDecimal value) {
        this.value = value;
    }

    public static CouponValue of(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 값은 0보다 커야 합니다.");
        }
        return new CouponValue(value);
    }

    public static CouponValue fixed(BigDecimal amount) {
        return of(amount);
    }

    public static CouponValue rate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인률은 1 미만이어야 합니다.");
        }
        return of(rate);
    }

    public BigDecimal getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponValue)) return false;
        CouponValue that = (CouponValue) o;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "CouponValue{" + "value=" + value + '}';
    }
}