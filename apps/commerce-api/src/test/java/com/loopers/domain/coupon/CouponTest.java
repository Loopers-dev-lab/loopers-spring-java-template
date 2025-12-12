package com.loopers.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("쿠폰 도메인 단위 테스트")
class CouponTest {

    @DisplayName("쿠폰 발급 시")
    @Nested
    class Issue {

        @DisplayName("정상적인 값으로 정액 쿠폰을 발급하면 성공한다")
        @Test
        void issueFixedCoupon_withValidInput_success() {
            // given
            String userId = "user1";
            String couponName = "1000원 할인 쿠폰";
            CouponType couponType = CouponType.FIXED;
            BigDecimal discountValue = BigDecimal.valueOf(1000);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            Coupon coupon = Coupon.issue(userId, couponName, couponType, discountValue, expiresAt);

            // then
            assertAll(
                    () -> assertThat(coupon.getUserId()).isEqualTo(userId),
                    () -> assertThat(coupon.getCouponName()).isEqualTo(couponName),
                    () -> assertThat(coupon.getCouponType()).isEqualTo(couponType),
                    () -> assertThat(coupon.getDiscountValue()).isEqualByComparingTo(discountValue),
                    () -> assertThat(coupon.isUsed()).isFalse(),
                    () -> assertThat(coupon.getExpiresAt()).isEqualTo(expiresAt)
            );
        }

        @DisplayName("정상적인 값으로 정률 쿠폰을 발급하면 성공한다")
        @Test
        void issueRateCoupon_withValidInput_success() {
            // given
            String userId = "user1";
            String couponName = "10% 할인 쿠폰";
            CouponType couponType = CouponType.RATE;
            BigDecimal discountValue = BigDecimal.valueOf(10); // 10%
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            Coupon coupon = Coupon.issue(userId, couponName, couponType, discountValue, expiresAt);

            // then
            assertAll(
                    () -> assertThat(coupon.getCouponType()).isEqualTo(CouponType.RATE),
                    () -> assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(10))
            );
        }

        @DisplayName("정률 쿠폰의 할인율이 100%를 초과하면 예외가 발생한다")
        @Test
        void issueRateCoupon_withOver100Percent_throwsException() {
            // given
            String userId = "user1";
            String couponName = "110% 할인 쿠폰";
            CouponType couponType = CouponType.RATE;
            BigDecimal discountValue = BigDecimal.valueOf(110);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when & then
            assertThatThrownBy(() -> Coupon.issue(userId, couponName, couponType, discountValue, expiresAt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정률 쿠폰의 할인율은 100% 이하여야 합니다");
        }

        @DisplayName("만료 시각이 현재보다 이전이면 예외가 발생한다")
        @Test
        void issueCoupon_withPastExpiresAt_throwsException() {
            // given
            String userId = "user1";
            String couponName = "만료된 쿠폰";
            CouponType couponType = CouponType.FIXED;
            BigDecimal discountValue = BigDecimal.valueOf(1000);
            LocalDateTime expiresAt = LocalDateTime.now().minusDays(1);

            // when & then
            assertThatThrownBy(() -> Coupon.issue(userId, couponName, couponType, discountValue, expiresAt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("만료 시각은 현재 시각 이후여야 합니다");
        }
    }

    @DisplayName("쿠폰 사용 시")
    @Nested
    class Use {

        @DisplayName("사용 가능한 쿠폰을 사용하면 사용 처리된다")
        @Test
        void useCoupon_withValidCoupon_success() {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "테스트 쿠폰",
                    CouponType.FIXED,
                    BigDecimal.valueOf(1000),
                    LocalDateTime.now().plusDays(7)
            );

            // when
            coupon.use();

            // then
            assertAll(
                    () -> assertThat(coupon.isUsed()).isTrue(),
                    () -> assertThat(coupon.getUsedAt()).isNotNull()
            );
        }

        @DisplayName("이미 사용된 쿠폰을 사용하려고 하면 예외가 발생한다")
        @Test
        void useCoupon_withUsedCoupon_throwsException() {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "테스트 쿠폰",
                    CouponType.FIXED,
                    BigDecimal.valueOf(1000),
                    LocalDateTime.now().plusDays(7)
            );
            coupon.use();

            // when & then
            assertThatThrownBy(coupon::use)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("사용할 수 없는 쿠폰입니다");
        }

        @DisplayName("만료된 쿠폰을 사용하려고 하면 예외가 발생한다")
        @Test
        void useCoupon_withExpiredCoupon_throwsException() throws InterruptedException {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "곧 만료될 쿠폰",
                    CouponType.FIXED,
                    BigDecimal.valueOf(1000),
                    LocalDateTime.now().plusSeconds(1)
            );

            // 쿠폰이 만료될 때까지 대기
            Thread.sleep(1100);

            // when & then
            assertThatThrownBy(coupon::use)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("사용할 수 없는 쿠폰입니다");
        }
    }

    @DisplayName("할인 금액 계산 시")
    @Nested
    class CalculateDiscount {

        @DisplayName("정액 쿠폰은 할인 값만큼 할인된다")
        @Test
        void calculateDiscount_withFixedCoupon_returnsDiscountValue() {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "1000원 할인 쿠폰",
                    CouponType.FIXED,
                    BigDecimal.valueOf(1000),
                    LocalDateTime.now().plusDays(7)
            );
            BigDecimal originalAmount = BigDecimal.valueOf(5000);

            // when
            BigDecimal discount = coupon.calculateDiscount(originalAmount);

            // then
            assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @DisplayName("정률 쿠폰은 원가의 할인율만큼 할인된다")
        @Test
        void calculateDiscount_withRateCoupon_returnsPercentageDiscount() {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "10% 할인 쿠폰",
                    CouponType.RATE,
                    BigDecimal.valueOf(10),
                    LocalDateTime.now().plusDays(7)
            );
            BigDecimal originalAmount = BigDecimal.valueOf(5000);

            // when
            BigDecimal discount = coupon.calculateDiscount(originalAmount);

            // then
            assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(500)); // 5000 * 0.1
        }

        @DisplayName("할인 금액이 원가를 초과할 수 없다")
        @Test
        void calculateDiscount_whenDiscountExceedsOriginal_returnsOriginalAmount() {
            // given
            Coupon coupon = Coupon.issue(
                    "user1",
                    "5000원 할인 쿠폰",
                    CouponType.FIXED,
                    BigDecimal.valueOf(5000),
                    LocalDateTime.now().plusDays(7)
            );
            BigDecimal originalAmount = BigDecimal.valueOf(3000);

            // when
            BigDecimal discount = coupon.calculateDiscount(originalAmount);

            // then
            assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(3000));
        }
    }
}
