package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.CouponDiscountRate;
import com.loopers.core.domain.payment.vo.PayAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RateDiscountCouponTest {

    @Nested
    @DisplayName("calculateDiscountAmount()")
    class CalculateDiscountAmount {

        @Nested
        @DisplayName("할인 금액은")
        class DiscountAmount {

            @Test
            @DisplayName("일정 비율을 가진다.")
            void discountAmount() {
                PayAmount payAmount = new PayAmount(new BigDecimal(10000));
                RateDiscountCoupon coupon = RateDiscountCouponFixture.createWith(new CouponDiscountRate(new BigDecimal(20)));

                assertThat(coupon.calculateDiscountAmount(payAmount)).isEqualByComparingTo(new BigDecimal(2000));
            }
        }
    }
}
