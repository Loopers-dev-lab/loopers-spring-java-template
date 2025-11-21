package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CouponTest {
  @Nested
  @DisplayName("쿠폰 할인 금액 계산 테스트")
  class CalculateDiscount {

    @Test
    @DisplayName("정액 쿠폰: 주문 금액에서 할인 금액만큼 차감된 값을 계산한다.")
    void fixed_amount_discount() {
      // given
      long orderAmount = 10000L;
      long discountAmount = 1000L;
      Coupon coupon = new Coupon(1l, CouponType.FIXED_AMOUNT, discountAmount);

      // when
      long result = coupon.calculateDiscountAmount(orderAmount);

      // then
      assertThat(result).isEqualTo(1000L);
    }

    @Test
    @DisplayName("정액 쿠폰: 주문 금액보다 할인 금액이 크면, 주문 금액만큼만 할인된다(결제금액 0원 보장).")
    void fixed_amount_discount_capped() {
      // given
      long orderAmount = 500L;
      long discountAmount = 1000L;
      Coupon coupon = new Coupon(1l, CouponType.FIXED_AMOUNT, discountAmount);

      // when
      long result = coupon.calculateDiscountAmount(orderAmount);

      // then
      assertThat(result).isEqualTo(500L);
    }

    @Test
    @DisplayName("정률 쿠폰: 주문 금액의 비율만큼 할인 금액이 계산된다.")
    void percentage_discount() {
      // given
      long orderAmount = 20000L;
      long discountRate = 10L; // 10%
      Coupon coupon = new Coupon(1l, CouponType.PERCENTAGE, discountRate);

      // when
      long result = coupon.calculateDiscountAmount(orderAmount);

      // then
      assertThat(result).isEqualTo(2000L);
    }
  }

  @Nested
  @DisplayName("쿠폰 생성 및 유효성 검증 테스트")
  class Validation {

    @Test
    @DisplayName("정률 쿠폰 생성 시 할인율이 100을 초과하면 예외가 발생한다.")
    void create_percentage_coupon_fail_over_100() {
      assertThatThrownBy(() ->
          new Coupon(1l, CouponType.PERCENTAGE, 101L)
      )
          .isInstanceOf(CoreException.class)
          .hasMessage("할인율은 0~100% 사이여야 합니다.");
    }
  }

  @Nested
  @DisplayName("쿠폰 사용 처리 테스트")
  class UseCoupon {

    @Test
    @DisplayName("쿠폰을 사용하면 used 상태가 true로 변경된다.")
    void use_coupon_success() {
      // given
      Coupon coupon = new Coupon(1l, CouponType.FIXED_AMOUNT, 1000L);

      // when
      coupon.use();

      // then
      assertThat(coupon.isUsed()).isTrue();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰을 다시 사용하려고 하면 예외가 발생한다.")
    void use_coupon_fail_already_used() {
      // given
      Coupon coupon = new Coupon(1l, CouponType.FIXED_AMOUNT, 1000L);
      coupon.use();

      // when & then
      assertThatThrownBy(coupon::use)
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
          .hasMessage("이미 사용된 쿠폰입니다.");
    }
  }
}
