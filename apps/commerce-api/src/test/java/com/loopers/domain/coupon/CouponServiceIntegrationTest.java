package com.loopers.domain.coupon;

import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.test.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CouponService 통합 테스트")
class CouponServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private CouponService couponService;

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private CouponPolicyRepository couponPolicyRepository;

  @Nested
  @DisplayName("쿠폰 사용 시")
  class UseCoupon {

    @Test
    @DisplayName("본인 소유의 AVAILABLE 쿠폰을 사용하면 USED 상태로 변경된다")
    void shouldChangeToUsed_whenValidCoupon() {
      // given
      Long userId = 1L;
      Long orderId = 100L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));

      // when
      Coupon usedCoupon = couponService.useCoupon(coupon.getId(), userId, orderId);

      // then
      assertThat(usedCoupon)
          .extracting("status", "usedOrderId")
          .containsExactly(CouponStatus.USED, orderId);
      assertThat(usedCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰을 사용하면 예외가 발생한다")
    void shouldThrowException_whenCouponNotFound() {
      // given
      Long nonExistentCouponId = 99999L;
      Long userId = 1L;
      Long orderId = 100L;

      // when & then
      assertThatThrownBy(() -> couponService.useCoupon(nonExistentCouponId, userId, orderId))
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인 소유가 아닌 쿠폰을 사용하면 예외가 발생한다")
    void shouldThrowException_whenNotOwned() {
      // given
      Long ownerId = 1L;
      Long anotherUserId = 2L;
      Long orderId = 100L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(ownerId, policy));

      // when & then
      assertThatThrownBy(() -> couponService.useCoupon(coupon.getId(), anotherUserId, orderId))
          .isInstanceOf(CoreException.class)
          .extracting("errorType")
          .isEqualTo(ErrorType.COUPON_NOT_OWNED);
    }

    @Test
    @DisplayName("이미 사용된 쿠폰을 다시 사용하면 예외가 발생한다")
    void shouldThrowException_whenAlreadyUsed() {
      // given
      Long userId = 1L;
      Long orderId = 100L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));
      couponService.useCoupon(coupon.getId(), userId, orderId);

      // when & then
      assertThatThrownBy(() -> couponService.useCoupon(coupon.getId(), userId, 200L))
          .isInstanceOf(CoreException.class)
          .extracting("errorType")
          .isEqualTo(ErrorType.COUPON_ALREADY_USED);
    }
  }

  @Nested
  @DisplayName("쿠폰 복구 시")
  class RestoreCoupon {

    @Test
    @DisplayName("USED 상태의 쿠폰을 복구하면 AVAILABLE 상태로 변경된다")
    void shouldChangeToAvailable_whenUsedCoupon() {
      // given
      Long userId = 1L;
      Long orderId = 100L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));
      couponService.useCoupon(coupon.getId(), userId, orderId);

      // when
      couponService.restoreCoupon(coupon.getId());

      // then
      Coupon restoredCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
      assertThat(restoredCoupon)
          .extracting("status", "usedAt", "usedOrderId")
          .containsExactly(CouponStatus.AVAILABLE, null, null);
    }

    @Test
    @DisplayName("AVAILABLE 상태의 쿠폰을 복구하면 예외가 발생한다")
    void shouldThrowException_whenAlreadyAvailable() {
      // given
      Long userId = 1L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));

      // when & then
      assertThatThrownBy(() -> couponService.restoreCoupon(coupon.getId()))
          .isInstanceOf(CoreException.class)
          .extracting("errorType")
          .isEqualTo(ErrorType.COUPON_NOT_USED);
    }
  }

  @Nested
  @DisplayName("할인 금액 계산 시")
  class CalculateDiscount {

    @Test
    @DisplayName("정액 쿠폰의 할인 금액을 계산한다")
    void shouldCalculateFixedDiscount() {
      // given
      Long userId = 1L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(5000L)));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));
      Money orderAmount = Money.of(10000L);

      // when
      Money discount = couponService.calculateDiscount(coupon.getId(), userId, orderAmount);

      // then
      assertThat(discount.getValue()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("정률 쿠폰의 할인 금액을 계산한다")
    void shouldCalculateRateDiscount() {
      // given
      Long userId = 1L;
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofRate(new BigDecimal("0.1")));
      Coupon coupon = couponRepository.save(Coupon.of(userId, policy));
      Money orderAmount = Money.of(10000L);

      // when
      Money discount = couponService.calculateDiscount(coupon.getId(), userId, orderAmount);

      // then
      assertThat(discount.getValue()).isEqualTo(1000L);
    }
  }
}
