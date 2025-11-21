package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discount {

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private PolicyType type;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @Column(name = "discount_rate")
    private Integer discountPercent;

    public static Discount createFixed(int amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }
        Discount info = new Discount();
        info.type = PolicyType.FIXED;
        info.discountAmount = amount;
        info.discountPercent = null;
        return info;
    }

    public static Discount createPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 퍼센트는 0 이상 100 이하여야 합니다.");
        }
        Discount info = new Discount();
        info.type = PolicyType.PERCENT;
        info.discountPercent = percent;
        info.discountAmount = null;
        return info;
    }

    public DiscountPolicy toPolicy() {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 정책 타입은 필수입니다.");
        }

        if (type == PolicyType.FIXED) {
            return new FixedAmountDiscountPolicy(discountAmount);
        }

        if (type == PolicyType.PERCENT) {
            return new PercentDiscountPolicy(discountPercent);
        }

        throw new CoreException(ErrorType.BAD_REQUEST, "알 수 없는 할인 정책 타입입니다.");
    }
}

