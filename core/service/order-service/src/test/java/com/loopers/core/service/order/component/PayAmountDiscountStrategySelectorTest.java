package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.vo.CouponId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("할인 전략 셀렉터")
class PayAmountDiscountStrategySelectorTest {

    @Nested
    @DisplayName("select()")
    class Select {

        @Nested
        @DisplayName("Coupon이 Null인 경우")
        class CouponIsNull {

            @Test
            @DisplayName("NoneDiscountStrategy를 반환한다.")
            void noneDiscountStrategy() {
                NoneDiscountStrategy noneDiscountStrategy = mock(NoneDiscountStrategy.class);
                CouponDiscountStrategy couponDiscountStrategy = mock(CouponDiscountStrategy.class);
                PayAmountDiscountStrategySelector selector = new PayAmountDiscountStrategySelector(
                        noneDiscountStrategy,
                        couponDiscountStrategy
                );

                PayAmountDiscountStrategy discountStrategy = selector.select(CouponId.empty());
                assertThat(discountStrategy).isExactlyInstanceOf(NoneDiscountStrategy.class);
            }
        }

        @Nested
        @DisplayName("쿠폰이 Null이 아닌 경우")
        class couponIsNotNull {

            @Test
            @DisplayName("CouponDiscountStrategy를 반환한다.")
            void couponDiscountStrategy() {
                NoneDiscountStrategy noneDiscountStrategy = mock(NoneDiscountStrategy.class);
                CouponDiscountStrategy couponDiscountStrategy = mock(CouponDiscountStrategy.class);
                PayAmountDiscountStrategySelector selector = new PayAmountDiscountStrategySelector(
                        noneDiscountStrategy,
                        couponDiscountStrategy
                );
                PayAmountDiscountStrategy discountStrategy = selector.select(new CouponId("1"));
                assertThat(discountStrategy).isExactlyInstanceOf(CouponDiscountStrategy.class);
            }
        }
    }
}
