package com.loopers.core.domain.order;

import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.payment.vo.PayAmount;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;

class AbstractCouponTest {

    @Nested
    @DisplayName("discount()")
    class DiscountMethod {

        @Nested
        @DisplayName("이미 사용된 쿠폰이면")
        class couponUnavailable {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                DefaultCoupon defaultCoupon = Instancio.of(DefaultCoupon.class)
                        .set(field(DefaultCoupon::getStatus), CouponStatus.USED)
                        .create();
                PayAmount payAmount = new PayAmount(new BigDecimal(1000));
                assertThatThrownBy(() -> defaultCoupon.discount(payAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("사용할 수 없는");
            }
        }
    }
}
