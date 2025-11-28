package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponTest {

    @DisplayName("쿠폰 생성")
    @Nested
    class CreateCoupon {

        @DisplayName("정률 할인 쿠폰을 생성한다.")
        @Test
        void createRateCoupon_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .build();

            // when
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "테스트를 위해 발급된 코드", "2025-01-01", "2025-12-31");

            // then
            assertAll(
                    () -> assertThat(coupon.getCode()).isEqualTo("TESTCODE123"),
                    () -> assertThat(coupon.getName()).isEqualTo("테스트 쿠폰"),
                    () -> assertThat(coupon.getDescription()).isEqualTo("테스트를 위해 발급된 코드"),
                    () -> assertThat(coupon.getValidStartDate()).isEqualTo("2025-01-01"),
                    () -> assertThat(coupon.getValidEndDate()).isEqualTo("2025-12-31"),
                    () -> assertThat(coupon.getDiscountPolicy()).isEqualTo(discountPolicy),
                    () -> assertThat(coupon.isActive()).isTrue(),
                    () -> assertThat(coupon.getCurrentIssuanceCount()).isZero()
            );
        }

        @DisplayName("정액 할인 쿠폰을 생성한다.")
        @Test
        void createAmountCoupon_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.AMOUNT)
                    .discountValue(5000)
                    .build();

            // when
            Coupon coupon = createCoupon(discountPolicy, "AMOUNT5000", "5000원 할인 쿠폰", "정액 할인 쿠폰", "2025-01-01", "2025-12-31");

            // then
            assertAll(
                    () -> assertThat(coupon.getCode()).isEqualTo("AMOUNT5000"),
                    () -> assertThat(coupon.getName()).isEqualTo("5000원 할인 쿠폰"),
                    () -> assertThat(coupon.getDiscountPolicy().getDiscountType()).isEqualTo(DiscountType.AMOUNT),
                    () -> assertThat(coupon.getDiscountPolicy().getDiscountValue()).isEqualTo(5000)
            );
        }
    }

    @DisplayName("쿠폰 코드 검증")
    @Nested
    class ValidateCouponCode {

        @DisplayName("쿠폰 코드가 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullCode_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, null, "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰코드는 필수값 입니다");
        }

        @DisplayName("쿠폰 코드에 특수문자가 포함되면 예외가 발생한다.")
        @Test
        void createCouponWithSpecialCharCode_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TEST_CODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰코드는 대문자 영문과 숫자로 10~20자여야 합니다");
        }

        @DisplayName("쿠폰 코드에 소문자가 포함되면 예외가 발생한다.")
        @Test
        void createCouponWithLowercaseCode_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "testcode123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰코드는 대문자 영문과 숫자로 10~20자여야 합니다");
        }

        @DisplayName("쿠폰 코드가 10자 미만이면 예외가 발생한다.")
        @Test
        void createCouponWithShortCode_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TEST123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰코드는 대문자 영문과 숫자로 10~20자여야 합니다");
        }

        @DisplayName("쿠폰 코드가 20자 초과이면 예외가 발생한다.")
        @Test
        void createCouponWithLongCode_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE1234567890123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰코드는 대문자 영문과 숫자로 10~20자여야 합니다");
        }
    }

    @DisplayName("쿠폰명 검증")
    @Nested
    class ValidateCouponName {

        @DisplayName("쿠폰명이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullName_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", null, "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰명은 필수값 입니다");
        }

        @DisplayName("쿠폰명이 공백이면 예외가 발생한다.")
        @Test
        void createCouponWithBlankName_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "   ", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰명은 필수값 입니다");
        }
    }

    @DisplayName("할인 정책 검증")
    @Nested
    class ValidateDiscountPolicy {

        @DisplayName("할인 정책이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullDiscountPolicy_throwException() {
            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(null, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("할인 정보는 필수값 입니다");
        }

        @DisplayName("할인 방식이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullDiscountType_throwException() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(null)
                    .discountValue(10)
                    .build();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("할인 방식은 필수값 입니다");
        }

        @DisplayName("할인값이 0이면 예외가 발생한다.")
        @Test
        void createCouponWithZeroDiscountValue_throwException() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(0)
                    .build();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("할인률(액)은 0보다 큰 양수여야 합니다");
        }

        @DisplayName("할인값이 음수이면 예외가 발생한다.")
        @Test
        void createCouponWithNegativeDiscountValue_throwException() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(-10)
                    .build();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("할인률(액)은 0보다 큰 양수여야 합니다");
        }
    }

    @DisplayName("유효기간 검증")
    @Nested
    class ValidateCouponDates {

        @DisplayName("유효 시작일이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullStartDate_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", null, "2025-12-31")
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 유효일은 필수값 입니다");
        }

        @DisplayName("유효 종료일이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullEndDate_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", null)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 유효일은 필수값 입니다");
        }
    }

    @DisplayName("쿠폰 발급 수량 관리")
    @Nested
    class IssuanceCountManagement {

        @DisplayName("쿠폰 발급 수량을 증가시킨다.")
        @Test
        void increaseIssuanceCount_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31");

            // when
            coupon.increaseIssuanceCount();

            // then
            assertThat(coupon.getCurrentIssuanceCount()).isEqualTo(1);
        }

        @DisplayName("발급 제한이 없으면 계속 발급할 수 있다.")
        @Test
        void increaseIssuanceCount_withoutLimit_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", "2025-01-01", "2025-12-31");

            // when
            for (int i = 0; i < 100; i++) {
                coupon.increaseIssuanceCount();
            }

            // then
            assertThat(coupon.getCurrentIssuanceCount()).isEqualTo(100);
        }

        @DisplayName("최대 발급 수량에 도달하면 예외가 발생한다.")
        @Test
        void increaseIssuanceCount_exceedLimit_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = Coupon.builder()
                    .code("TESTCODE123")
                    .name("테스트 쿠폰")
                    .description("설명")
                    .validStartDate("2025-01-01")
                    .validEndDate("2025-12-31")
                    .discountPolicy(discountPolicy)
                    .build();

            // maxIssuanceLimit을 3으로 설정 (리플렉션 사용)
            try {
                var field = Coupon.class.getDeclaredField("maxIssuanceLimit");
                field.setAccessible(true);
                field.set(coupon, 3);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // when
            coupon.increaseIssuanceCount(); // 1
            coupon.increaseIssuanceCount(); // 2
            coupon.increaseIssuanceCount(); // 3

            // then
            CoreException result = assertThrows(CoreException.class, coupon::increaseIssuanceCount);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰이 모두 소진되었습니다.");
        }
    }

    private static Coupon createCoupon(
            DiscountPolicy discountPolicy, String code, String name,
            String desc, String validStartDate, String validEndDate) {
        return Coupon.builder()
                .code(code)
                .name(name)
                .description(desc)
                .validStartDate(validStartDate)
                .validEndDate(validEndDate)
                .discountPolicy(discountPolicy)
                .build();
    }

    private static DiscountPolicy createValidDiscountPolicy() {
        return DiscountPolicy.builder()
                .discountType(DiscountType.RATE)
                .discountValue(10)
                .build();
    }
}
