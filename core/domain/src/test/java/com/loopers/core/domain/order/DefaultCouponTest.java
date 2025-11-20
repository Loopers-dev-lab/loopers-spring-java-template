package com.loopers.core.domain.order;

import com.loopers.core.domain.user.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.loopers.core.domain.order.type.CouponStatus.AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultCouponTest {

    @Nested
    @DisplayName("create()")
    class CreateMethod {

        @Nested
        @DisplayName("정상 생성되었을때")
        class DefaultValue {

            @Test
            @DisplayName("기본값을 가진다.")
            void hasDefaultValue() {
                DefaultCoupon coupon = DefaultCoupon.create(
                        new UserId("1")
                );

                assertThat(coupon.status).isEqualTo(AVAILABLE);
            }
        }
    }

}
