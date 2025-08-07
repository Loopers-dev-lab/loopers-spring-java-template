package com.loopers.domain.coupon;

import com.loopers.domain.coupon.fixture.CouponFixture;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CouponModel 테스트")
class CouponModelTest {

    @Test
    @DisplayName("정액 할인 쿠폰을 생성할 수 있다")
    void createFixedCoupon() {
        // arrange & act
        CouponModel coupon = CouponFixture.createFixedCoupon();

        // assert
        assertThat(coupon.getUserId().getValue()).isEqualTo(1L);
        assertThat(coupon.getType().getValue().name()).isEqualTo("FIXED");
        assertThat(coupon.getValue().getValue()).isEqualTo(new BigDecimal("5000"));
        assertThat(coupon.getUsed().isUsed()).isFalse();
        assertThat(coupon.getOrderId().getValue()).isNull();
        assertThat(coupon.canUse()).isTrue();
    }

    @Test
    @DisplayName("정률 할인 쿠폰을 생성할 수 있다")
    void createRateCoupon() {
        // arrange & act
        CouponModel coupon = CouponFixture.createRateCoupon();

        // assert
        assertThat(coupon.getUserId().getValue()).isEqualTo(1L);
        assertThat(coupon.getType().getValue().name()).isEqualTo("RATE");
        assertThat(coupon.getValue().getValue()).isEqualTo(new BigDecimal("0.1"));
        assertThat(coupon.getUsed().isUsed()).isFalse();
        assertThat(coupon.getOrderId().getValue()).isNull();
        assertThat(coupon.canUse()).isTrue();
    }

    @Test
    @DisplayName("새로 생성된 쿠폰은 사용 가능하다")
    void newCouponCanUse() {
        // arrange
        CouponModel coupon = CouponFixture.createFixedCoupon();

        // act & assert
        assertThat(coupon.canUse()).isTrue();
        assertThat(coupon.isExpired()).isFalse();
        assertThat(coupon.isUsed()).isFalse();
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용할 수 없다")
    void expiredCouponCannotBeUsed() {
        // arrange
//        CouponModel coupon = CouponFixture.createFixedCoupon();
//
//        // act & assert
//        assertThat(coupon.canUse()).isFalse();
//        assertThat(coupon.isExpired()).isTrue();
//        assertThat(coupon.isUsed()).isFalse();

    }

    @Test
    @DisplayName("쿠폰을 사용하면 사용 상태로 변경된다")
    void useCoupon() {
        // arrange
        CouponModel coupon = CouponFixture.createFixedCoupon();
        Long orderId = 100L;

        // act
        coupon.use(orderId);

        // assert
        assertThat(coupon.isUsed()).isTrue();
        assertThat(coupon.getOrderId().getValue()).isEqualTo(orderId);
        assertThat(coupon.canUse()).isFalse();
    }

    @Test
    @DisplayName("이미 사용된 쿠폰은 다시 사용할 수 없다")
    void usedCouponCannotUseAgain() {
        // arrange
        CouponModel coupon = CouponFixture.createUsedFixedCoupon();

        // act & assert
        assertThatThrownBy(() -> coupon.use(200L))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("만료된 쿠폰을 사용하려고 하면 예외가 발생한다")
    void throwExceptionWhenUsingExpiredCoupon() {
//        // arrange
//        CouponModel coupon = CouponFixture.createFixedCoupon();
//
//        coupon.getExpiredAt().getValue().minusDays(1000);
//
//        // act & assert
//        assertThatThrownBy(() -> coupon.use(100L))
//                .isInstanceOf(CoreException.class)
//                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("정액 할인 쿠폰의 할인 금액을 계산할 수 있다")
    void calculateFixedDiscountAmount() {
        // arrange
        CouponModel coupon = CouponFixture.createFixedCoupon();
        BigDecimal originalAmount = new BigDecimal("10000");

        // act
        BigDecimal discountAmount = coupon.calculateDiscountAmount(originalAmount);

        // assert
        assertThat(discountAmount).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("정액 할인 쿠폰의 할인 금액이 원래 금액보다 클 때는 원래 금액만큼 할인된다")
    void fixedDiscountCannotExceedOriginalAmount() {
        // arrange
        CouponModel coupon = CouponFixture.createFixedCouponWithAmount(new BigDecimal("10000"));
        BigDecimal originalAmount = new BigDecimal("5000");

        // act
        BigDecimal discountAmount = coupon.calculateDiscountAmount(originalAmount);

        // assert
        assertThat(discountAmount).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("정률 할인 쿠폰의 할인 금액을 계산할 수 있다")
    void calculateRateDiscountAmount() {
        // arrange
        CouponModel coupon = CouponFixture.createRateCoupon(); // 10% 할인
        BigDecimal originalAmount = new BigDecimal("10000");

        // act
        BigDecimal discountAmount = coupon.calculateDiscountAmount(originalAmount);

        // assert
        assertThat(discountAmount).isEqualTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("정률 할인시 소수점 이하는 버림 처리된다")
    void rateDiscountRoundDown() {
        // arrange
        CouponModel coupon = CouponFixture.createRateCouponWithRate(new BigDecimal("0.15")); // 15% 할인
        BigDecimal originalAmount = new BigDecimal("1000");

        // act
        BigDecimal discountAmount = coupon.calculateDiscountAmount(originalAmount);

        // assert - 1000 * 0.15 = 150 (소수점 없음)
        assertThat(discountAmount).isEqualTo(new BigDecimal("150"));
    }

    @Test
    @DisplayName("사용할 수 없는 쿠폰의 할인 금액은 계산할 수 없다")
    void cannotCalculateDiscountForUnusableCoupon() {
//        // arrange
//        CouponModel coupon = CouponFixture.createExpiredFixedCoupon();
//        BigDecimal originalAmount = new BigDecimal("10000");
//
//        // act & assert
//        assertThatThrownBy(() -> coupon.calculateDiscountAmount(originalAmount))
//                .isInstanceOf(CoreException.class)
//                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰이 특정 사용자에게 속하는지 확인할 수 있다")
    void belongsToUser() {
        // arrange
        Long userId = 1L;
        CouponModel coupon = CouponFixture.createFixedCouponWithUserId(userId);

        // act & assert
        assertThat(coupon.belongsToUser(userId)).isTrue();
        assertThat(coupon.belongsToUser(2L)).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급일은 현재 시각 이후여야 한다")
    void issuedAtShouldNotBeInPast() {
        // arrange
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("5000");
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        // act & assert
        assertThatThrownBy(() -> CouponModel.createFixedWithIssueDate(userId, amount, pastTime, expiredAt))
                .isInstanceOf(CoreException.class)
                .hasMessage("발급일은 현재 이후여야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 발급일이 현재 시각이면 정상적으로 생성된다")
    void issuedAtCanBeCurrentTime() {
        // arrange
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("5000");
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30);
        LocalDateTime currentTime = LocalDateTime.now();

        // act
        CouponModel coupon = CouponModel.createFixedWithIssueDate(userId, amount, currentTime, expiredAt);

        // assert
        assertThat(coupon).isNotNull();
        assertThat(coupon.getIssuedAt().getValue()).isEqualTo(currentTime);
    }
}
