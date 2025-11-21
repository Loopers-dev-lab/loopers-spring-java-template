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
     * CouponDiscountStrategyFactory를 생성합니다.
     *
     * @param fixedAmountStrategy 정액 쿠폰 전략
     * @param percentageStrategy 정률 쿠폰 전략
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
     * 쿠폰 타입에 해당하는 할인 계산 전략을 반환합니다.
     *
     * @param type 쿠폰 타입
     * @return 할인 계산 전략
     * @throws IllegalArgumentException 지원하지 않는 쿠폰 타입인 경우
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

