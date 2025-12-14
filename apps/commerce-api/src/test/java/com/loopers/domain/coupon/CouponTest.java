package com.loopers.domain.coupon;

import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Coupon 도메인 테스트")
class CouponTest {

  private static final Long USER_ID = 1L;
  private static final Long ORDER_ID = 100L;
  private static final LocalDateTime USED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 12, 0, 0);

  @DisplayName("Coupon 생성")
  @Nested
  class Create {

    @DisplayName("유효한 정보로 쿠폰을 생성하면 AVAILABLE 상태로 생성된다")
    @Test
    void shouldCreate_whenValid() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));

      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThat(coupon)
          .extracting("userId", "status", "usedAt", "usedOrderId")
          .containsExactly(USER_ID, CouponStatus.AVAILABLE, null, null);
    }

    @DisplayName("userId가 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenUserIdNull() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));

      assertThatThrownBy(() -> Coupon.of(null, policy))
          .isInstanceOf(CoreException.class)
          .hasMessage("쿠폰 소유자는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_COUPON_USER_EMPTY);
    }

    @DisplayName("couponPolicy가 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenPolicyNull() {
      assertThatThrownBy(() -> Coupon.of(USER_ID, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("쿠폰 정책은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_COUPON_POLICY_EMPTY);
    }
  }

  @DisplayName("쿠폰 사용 처리 (toUsed)")
  @Nested
  class ToUsed {

    @DisplayName("AVAILABLE 상태의 쿠폰을 사용 처리하면 USED 상태가 된다")
    @Test
    void shouldChangeToUsed_whenAvailable() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      coupon.toUsed(ORDER_ID, USED_AT_2025_10_30);

      assertThat(coupon)
          .extracting("status", "usedAt", "usedOrderId")
          .containsExactly(CouponStatus.USED, USED_AT_2025_10_30, ORDER_ID);
    }

    @DisplayName("이미 USED 상태인 쿠폰을 사용하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenAlreadyUsed() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);
      coupon.toUsed(ORDER_ID, USED_AT_2025_10_30);

      assertThatThrownBy(() -> coupon.toUsed(200L, LocalDateTime.now()))
          .isInstanceOf(CoreException.class)
          .hasMessage("이미 사용된 쿠폰입니다.")
          .extracting("errorType").isEqualTo(ErrorType.COUPON_ALREADY_USED);
    }

    @DisplayName("orderId가 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenOrderIdNull() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThatThrownBy(() -> coupon.toUsed(null, USED_AT_2025_10_30))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("주문 ID는 null일 수 없습니다.");
    }

    @DisplayName("usedAt이 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenUsedAtNull() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThatThrownBy(() -> coupon.toUsed(ORDER_ID, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("사용 시각은 null일 수 없습니다.");
    }
  }

  @DisplayName("쿠폰 복구 처리 (toAvailable)")
  @Nested
  class ToAvailable {

    @DisplayName("USED 상태의 쿠폰을 복구하면 AVAILABLE 상태가 된다")
    @Test
    void shouldChangeToAvailable_whenUsed() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);
      coupon.toUsed(ORDER_ID, USED_AT_2025_10_30);

      coupon.toAvailable();

      assertThat(coupon)
          .extracting("status", "usedAt", "usedOrderId")
          .containsExactly(CouponStatus.AVAILABLE, null, null);
    }

    @DisplayName("AVAILABLE 상태의 쿠폰을 복구하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenAlreadyAvailable() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThatThrownBy(coupon::toAvailable)
          .isInstanceOf(CoreException.class)
          .hasMessage("사용된 쿠폰만 복구할 수 있습니다.")
          .extracting("errorType").isEqualTo(ErrorType.COUPON_NOT_USED);
    }
  }

  @DisplayName("소유자 검증 (isOwnedBy)")
  @Nested
  class IsOwnedBy {

    @DisplayName("소유자 userId와 일치하면 true를 반환한다")
    @Test
    void shouldReturnTrue_whenOwner() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThat(coupon.isOwnedBy(USER_ID)).isTrue();
    }

    @DisplayName("소유자 userId와 일치하지 않으면 false를 반환한다")
    @Test
    void shouldReturnFalse_whenNotOwner() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThat(coupon.isOwnedBy(999L)).isFalse();
    }

    @DisplayName("userId가 null이면 false를 반환한다")
    @Test
    void shouldReturnFalse_whenUserIdNull() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThat(coupon.isOwnedBy(null)).isFalse();
    }
  }

  @DisplayName("사용 가능 여부 (isAvailable)")
  @Nested
  class IsAvailable {

    @DisplayName("AVAILABLE 상태이면 true를 반환한다")
    @Test
    void shouldReturnTrue_whenAvailable() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);

      assertThat(coupon.isAvailable()).isTrue();
    }

    @DisplayName("USED 상태이면 false를 반환한다")
    @Test
    void shouldReturnFalse_whenUsed() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);
      coupon.toUsed(ORDER_ID, USED_AT_2025_10_30);

      assertThat(coupon.isAvailable()).isFalse();
    }
  }

  @DisplayName("할인 금액 계산 (calculateDiscount)")
  @Nested
  class CalculateDiscount {

    @DisplayName("쿠폰 정책에 할인 계산을 위임한다")
    @Test
    void shouldDelegateToPolicy() {
      CouponPolicy policy = CouponPolicy.ofFixed(Money.of(5000L));
      Coupon coupon = Coupon.of(USER_ID, policy);
      Money orderAmount = Money.of(10000L);

      Money discount = coupon.calculateDiscount(orderAmount);

      assertThat(discount.getValue()).isEqualTo(5000L);
    }
  }
}
