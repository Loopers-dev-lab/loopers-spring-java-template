package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.CouponDiscountAmount;
import com.loopers.core.domain.payment.vo.PayAmount;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

@DisplayName("정액 할인 쿠폰")
class AmountDiscountCouponTest {

    @Nested
    @DisplayName("calculateDiscountAmount()")
    class CalculateDiscountAmount {

        @Nested
        @DisplayName("할인 금액은")
        class DiscountAmount {

            @Test
            @DisplayName("일정 양을 가진다.")
            void discountAmount() {
                PayAmount payAmount = new PayAmount(new BigDecimal(10000));
                AmountDiscountCoupon coupon = Instancio.of(AmountDiscountCoupon.class)
                        .set(field(AmountDiscountCoupon::getAmount), new CouponDiscountAmount(new BigDecimal(1000)))
                        .create();

                assertThat(coupon.calculateDiscountAmount(payAmount)).isEqualByComparingTo(new BigDecimal(1000));
            }
        }
    }
}
