package com.loopers.core.domain.order.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("쿠폰 할인율")
class CouponDiscountRateTest {

    @Nested
    @DisplayName("쿠폰 할인율 생성 시")
    class 쿠폰_할인율_생성 {

        @Nested
        @DisplayName("값이 null인 경우")
        class 값이_null인_경우 {

            @Test
            @DisplayName("NullPointerException이 발생한다.")
            void NullPointerException이_발생한다() {
                assertThatThrownBy(() -> new CouponDiscountRate(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("값이 0 미만인 경우")
        class 값이_0_미만인_경우 {

            @Test
            @DisplayName("IllegalArgumentException이 발생한다.")
            void IllegalArgumentException이_발생한다() {
                assertThatThrownBy(() -> new CouponDiscountRate(new BigDecimal("-0.01")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("할인율은 0~100 사이여야 합니다");
            }
        }

        @Nested
        @DisplayName("값이 100을 초과하는 경우")
        class 값이_100을_초과하는_경우 {

            @Test
            @DisplayName("IllegalArgumentException이 발생한다.")
            void IllegalArgumentException이_발생한다() {
                assertThatThrownBy(() -> new CouponDiscountRate(new BigDecimal("100.01")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("할인율은 0~100 사이여야 합니다");
            }
        }

        @Nested
        @DisplayName("유효한 할인율이 주어진 경우")
        class 유효한_할인율이_주어진_경우 {

            @Test
            @DisplayName("쿠폰 할인율이 생성된다.")
            void 쿠폰_할인율이_생성된다() {
                CouponDiscountRate rate = new CouponDiscountRate(new BigDecimal("15.50"));

                assertThat(rate.value()).isEqualByComparingTo(new BigDecimal("15.50"));
            }

            @Test
            @DisplayName("최소값(0)으로 생성된다.")
            void 최소값으로_생성된다() {
                CouponDiscountRate rate = new CouponDiscountRate(BigDecimal.ZERO);

                assertThat(rate.value()).isEqualByComparingTo(BigDecimal.ZERO);
            }

            @Test
            @DisplayName("최대값(100)으로 생성된다.")
            void 최대값으로_생성된다() {
                CouponDiscountRate rate = new CouponDiscountRate(new BigDecimal("100.00"));

                assertThat(rate.value()).isEqualByComparingTo(new BigDecimal("100.00"));
            }

            @Test
            @DisplayName("정수값으로 생성된다.")
            void 정수값으로_생성된다() {
                CouponDiscountRate rate = new CouponDiscountRate(new BigDecimal("10"));

                assertThat(rate.value()).isEqualByComparingTo(new BigDecimal("10"));
            }
        }
    }
}
