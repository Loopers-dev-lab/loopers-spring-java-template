package com.loopers.domain.coupon;

import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CouponPolicy 도메인 테스트")
class CouponPolicyTest {

  @DisplayName("정액 쿠폰 정책 생성")
  @Nested
  class CreateFixed {

    @DisplayName("정액 할인 금액이 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenFixedAmountNull() {
      assertThatThrownBy(() -> CouponPolicy.ofFixed(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("정액 할인 금액은 null일 수 없습니다.");
    }

    @DisplayName("정액 할인 금액으로 정책을 생성한다")
    @Test
    void shouldCreate_whenValidFixedAmount() {
      Money discountAmount = Money.of(5000L);

      CouponPolicy policy = CouponPolicy.ofFixed(discountAmount);

      assertThat(policy)
          .extracting("discountType", "discountAmount.value", "discountRate")
          .containsExactly(CouponDiscountType.FIXED, 5000L, null);
    }
  }

  @DisplayName("정률 쿠폰 정책 생성")
  @Nested
  class CreateRate {

    @DisplayName("정률 할인율이 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenRateNull() {
      assertThatThrownBy(() -> CouponPolicy.ofRate(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("정률 할인율은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_COUPON_POLICY_DISCOUNT_RATE_EMPTY);
    }

    @DisplayName("정률 할인율이 유효 범위(0 < rate <= 1)를 벗어나면 예외가 발생한다")
    @ParameterizedTest(name = "할인율 {0}이면 예외 발생")
    @ValueSource(strings = {"0", "-0.1", "1.1", "-1", "2"})
    void shouldThrowException_whenRateOutOfRange(String rateValue) {
      BigDecimal rate = new BigDecimal(rateValue);

      assertThatThrownBy(() -> CouponPolicy.ofRate(rate))
          .isInstanceOf(CoreException.class)
          .hasMessage("할인율은 0 초과 1 이하여야 합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_COUPON_POLICY_DISCOUNT_RATE_RANGE);
    }

    @DisplayName("정률 할인율이 경계값(1)이면 정상 생성된다")
    @Test
    void shouldCreate_whenRateExactlyOne() {
      CouponPolicy policy = CouponPolicy.ofRate(BigDecimal.ONE);

      assertThat(policy.getDiscountRate()).isEqualTo(BigDecimal.ONE);
    }

    @DisplayName("정률 할인율로 정책을 생성한다")
    @Test
    void shouldCreate_whenValidRate() {
      BigDecimal discountRate = new BigDecimal("0.1");

      CouponPolicy policy = CouponPolicy.ofRate(discountRate);

      assertThat(policy)
          .extracting("discountType", "discountAmount", "discountRate")
          .containsExactly(CouponDiscountType.RATE, null, discountRate);
    }
  }

  @DisplayName("할인 금액 계산")
  @Nested
  class CalculateDiscount {

    @DisplayName("주문 금액이 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenOrderAmountNull() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));

      assertThatThrownBy(() -> policy.calculateDiscount(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("주문 금액은 null일 수 없습니다.");
    }

    @DisplayName("정액 쿠폰: 할인 금액과 주문 금액 중 작은 값을 반환한다")
    @ParameterizedTest(name = "주문금액 {0}, 할인금액 {1} → 결과 {2}")
    @CsvSource({
        "10000, 5000, 5000",   // 주문금액 > 할인금액 → 할인금액 반환
        "10000, 15000, 10000", // 주문금액 < 할인금액 → 주문금액 반환
        "10000, 10000, 10000"  // 주문금액 = 할인금액 → 동일값 반환
    })
    void shouldReturnMinValue_whenFixed(Long orderAmount, Long discountAmount, Long expected) {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(discountAmount));

      Money discount = policy.calculateDiscount(Money.of(orderAmount));

      assertThat(discount.getValue()).isEqualTo(expected);
    }

    @DisplayName("정률 쿠폰: 주문 금액에 할인율을 적용한 금액을 반환한다")
    @ParameterizedTest(name = "주문금액 {0} × 할인율 {1} = {2}")
    @CsvSource({
        "10000, 0.1, 1000",   // 10% 할인
        "10000, 0.5, 5000",   // 50% 할인
        "10003, 0.15, 1500"   // 소수점 이하 내림 처리
    })
    void shouldReturnCalculatedDiscount_whenRate(Long orderAmount, String rate, Long expected) {
      CouponPolicy policy = CouponPolicy.ofRate(new BigDecimal(rate));

      Money discount = policy.calculateDiscount(Money.of(orderAmount));

      assertThat(discount.getValue()).isEqualTo(expected);
    }
  }
}
