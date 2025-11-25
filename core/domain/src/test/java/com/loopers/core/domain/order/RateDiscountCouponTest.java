package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.CouponDiscountRate;
import com.loopers.core.domain.payment.vo.PayAmount;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

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
                RateDiscountCoupon coupon = Instancio.of(RateDiscountCoupon.class)
                        .set(field(RateDiscountCoupon::getRate), new CouponDiscountRate(new BigDecimal(20)))
                        .create();

                assertThat(coupon.calculateDiscountAmount(payAmount)).isEqualByComparingTo(new BigDecimal(2000));
            }
        }
    }
}
