package com.loopers.domain.coupon;

import com.loopers.domain.Money;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode
public class DiscountPolicy {
    private DiscountType discountType;
    private int discountValue;

    @Builder
    public DiscountPolicy(DiscountType discountType, int discountValue) {
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    /**
     * 할인 금액을 계산합니다.
     * @param originalPrice 원래 금액
     * @return 할인 금액
     */
    public Money calculateDiscount(Money originalPrice) {
        if (discountType == DiscountType.RATE) {
            // 정률 할인: 원래 금액 * (할인율 / 100)
            BigDecimal discountRate = BigDecimal.valueOf(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = originalPrice.getAmount().multiply(discountRate);
            return Money.of(discountAmount);
        } else {
            // 정액 할인: 고정 금액
            return Money.of(discountValue);
        }
    }

    /**
     * 할인을 적용한 최종 금액을 계산합니다.
     * @param originalPrice 원래 금액
     * @return 할인 적용 후 금액 (최소 0원)
     */
    public Money applyDiscount(Money originalPrice) {
        Money discountAmount = calculateDiscount(originalPrice);

        // 할인 금액이 원래 금액보다 크면 0원 반환
        if (discountAmount.isGreaterThanOrEqual(originalPrice)) {
            return Money.zero();
        }

        return originalPrice.subtract(discountAmount);
    }
}
