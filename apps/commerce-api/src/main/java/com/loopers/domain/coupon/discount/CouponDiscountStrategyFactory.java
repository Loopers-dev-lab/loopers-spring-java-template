package com.loopers.domain.coupon.discount;

import com.loopers.domain.coupon.CouponType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 쿠폰 할인 계산 전략 팩토리.
 * <p>
 * 쿠폰 타입에 따라 적절한 할인 계산 전략을 반환합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Component
public class CouponDiscountStrategyFactory {
    private final Map<CouponType, CouponDiscountStrategy> strategyMap;

    /**
     * Create a factory that maps coupon types to their corresponding discount strategies.
     *
     * @param fixedAmountStrategy strategy used for CouponType.FIXED_AMOUNT
     * @param percentageStrategy  strategy used for CouponType.PERCENTAGE
     */
    public CouponDiscountStrategyFactory(
        FixedAmountDiscountStrategy fixedAmountStrategy,
        PercentageDiscountStrategy percentageStrategy
    ) {
        this.strategyMap = Map.of(
            CouponType.FIXED_AMOUNT, fixedAmountStrategy,
            CouponType.PERCENTAGE, percentageStrategy
        );
    }

    /**
     * Selects the discount calculation strategy for the given coupon type.
     *
     * @param type the coupon type to select a strategy for
     * @return the {@link CouponDiscountStrategy} associated with the specified coupon type
     * @throws IllegalArgumentException if no strategy is registered for the provided coupon type
     */
    public CouponDiscountStrategy getStrategy(CouponType type) {
        CouponDiscountStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                String.format("지원하지 않는 쿠폰 타입입니다. (타입: %s)", type));
        }
        return strategy;
    }
}
