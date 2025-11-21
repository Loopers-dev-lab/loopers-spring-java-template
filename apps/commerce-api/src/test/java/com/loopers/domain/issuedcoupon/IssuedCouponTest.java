package com.loopers.domain.issuedcoupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.DiscountPolicy;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssuedCouponTest {

    @DisplayName("쿠폰 발급")
    @Nested
    class IssueCoupon {

        @DisplayName("유효한 사용자와 활성화된 쿠폰으로 쿠폰을 발급한다.")
        @Test
        void issueCoupon_success() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();

            // when
            IssuedCoupon issuedCoupon = IssuedCoupon.issue(user, coupon);

            // then
            assertAll(
                    () -> assertThat(issuedCoupon.getUser()).isEqualTo(user),
                    () -> assertThat(issuedCoupon.getCoupon()).isEqualTo(coupon),
                    () -> assertThat(issuedCoupon.getStatus()).isEqualTo(CouponStatus.USABLE),
                    () -> assertThat(issuedCoupon.getUsedAt()).isNull()
            );
        }

        @DisplayName("쿠폰이 null이면 예외가 발생한다.")
        @Test
        void issueCoupon_withNullCoupon_throwException() {
            // given
            User user = createValidUser();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    IssuedCoupon.issue(user, null)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰은 필수입니다");
        }

        @DisplayName("비활성화된 쿠폰으로 발급 시도 시 예외가 발생한다.")
        @Test
        void issueCoupon_withInactiveCoupon_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();

            // 쿠폰을 비활성화 (리플렉션 사용)
            setInactiveCoupon(coupon);

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    IssuedCoupon.issue(user, coupon)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("비활성화된 쿠폰입니다");
        }
    }

    @DisplayName("발급 쿠폰 필드 검증")
    @Nested
    class ValidateIssuedCouponFields {

        @DisplayName("사용자가 null이면 예외가 발생한다.")
        @Test
        void createIssuedCoupon_withNullUser_throwException() {
            // given
            Coupon coupon = createValidCoupon();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    IssuedCoupon.builder()
                            .user(null)
                            .coupon(coupon)
                            .status(CouponStatus.USABLE)
                            .build()
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("사용자는 필수입니다");
        }

        @DisplayName("쿠폰이 null이면 예외가 발생한다.")
        @Test
        void createIssuedCoupon_withNullCoupon_throwException() {
            // given
            User user = createValidUser();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    IssuedCoupon.builder()
                            .user(user)
                            .coupon(null)
                            .status(CouponStatus.USABLE)
                            .build()
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰은 필수입니다");
        }

        @DisplayName("쿠폰 상태가 null이면 예외가 발생한다.")
        @Test
        void createIssuedCoupon_withNullStatus_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    IssuedCoupon.builder()
                            .user(user)
                            .coupon(coupon)
                            .status(null)
                            .build()
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 상태는 필수입니다");
        }
    }

    @DisplayName("쿠폰 사용")
    @Nested
    class UseCoupon {

        @DisplayName("USABLE 상태의 쿠폰을 사용한다.")
        @Test
        void useCoupon_success() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();
            IssuedCoupon issuedCoupon = IssuedCoupon.issue(user, coupon);

            // when
            issuedCoupon.useCoupon();

            // then
            assertAll(
                    () -> assertThat(issuedCoupon.getStatus()).isEqualTo(CouponStatus.USED),
                    () -> assertThat(issuedCoupon.getUsedAt()).isNotNull(),
                    () -> assertThat(issuedCoupon.getUsedAt()).isBeforeOrEqualTo(LocalDateTime.now())
            );
        }

        @DisplayName("이미 사용된 쿠폰을 다시 사용하면 예외가 발생한다.")
        @Test
        void useCoupon_alreadyUsed_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();
            IssuedCoupon issuedCoupon = IssuedCoupon.issue(user, coupon);
            issuedCoupon.useCoupon(); // 첫 번째 사용

            // when & then
            CoreException result = assertThrows(CoreException.class, issuedCoupon::useCoupon);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(result.getCustomMessage()).isEqualTo("이미 사용된 쿠폰입니다");
        }

        @DisplayName("만료된 쿠폰을 사용하면 예외가 발생한다.")
        @Test
        void useCoupon_expired_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();
            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .user(user)
                    .coupon(coupon)
                    .status(CouponStatus.EXPIRED)
                    .build();

            // when & then
            CoreException result = assertThrows(CoreException.class, issuedCoupon::useCoupon);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(result.getCustomMessage()).isEqualTo("이미 만료된 쿠폰입니다");
        }

        @DisplayName("상태가 null인 쿠폰을 사용하면 예외가 발생한다.")
        @Test
        void useCoupon_withNullStatus_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();
            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .user(user)
                    .coupon(coupon)
                    .status(CouponStatus.USABLE)
                    .build();

            // status를 null로 설정 (리플렉션 사용)
            setNullStatus(issuedCoupon);

            // when & then
            CoreException result = assertThrows(CoreException.class, issuedCoupon::useCoupon);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 상태가 올바르지 않습니다");
        }

        @DisplayName("이미 usedAt이 설정된 쿠폰을 사용하면 예외가 발생한다.")
        @Test
        void useCoupon_withAlreadySetUsedAt_throwException() {
            // given
            User user = createValidUser();
            Coupon coupon = createValidCoupon();
            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .user(user)
                    .coupon(coupon)
                    .status(CouponStatus.USABLE)
                    .build();

            // usedAt을 미리 설정 (리플렉션 사용)
            setUsedAt(issuedCoupon, LocalDateTime.now());

            // when & then
            CoreException result = assertThrows(CoreException.class, issuedCoupon::useCoupon);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(result.getCustomMessage()).isEqualTo("이미 사용된 쿠폰입니다");
        }
    }

    private static User createValidUser() {
        return User.builder()
                .userId("testuser1")
                .email("test@example.com")
                .birthdate("1990-01-01")
                .gender(Gender.MALE)
                .build();
    }

    private static Coupon createValidCoupon() {
        DiscountPolicy discountPolicy = DiscountPolicy.builder()
                .discountType(DiscountType.RATE)
                .discountValue(10)
                .build();

        return Coupon.builder()
                .code("TESTCODE123")
                .name("테스트 쿠폰")
                .description("테스트용 쿠폰")
                .validStartDate("2025-01-01")
                .validEndDate("2025-12-31")
                .discountPolicy(discountPolicy)
                .build();
    }

    private static void setInactiveCoupon(Coupon coupon) {
        try {
            Field field = Coupon.class.getDeclaredField("isActive");
            field.setAccessible(true);
            field.set(coupon, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setNullStatus(IssuedCoupon issuedCoupon) {
        try {
            Field field = IssuedCoupon.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(issuedCoupon, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setUsedAt(IssuedCoupon issuedCoupon, LocalDateTime usedAt) {
        try {
            Field field = IssuedCoupon.class.getDeclaredField("usedAt");
            field.setAccessible(true);
            field.set(issuedCoupon, usedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
