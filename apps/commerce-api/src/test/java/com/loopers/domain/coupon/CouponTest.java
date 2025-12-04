package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "테스트를 위해 발급된 코드",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // then
            assertAll(
                    () -> assertThat(coupon.getCode()).isEqualTo("TESTCODE123"),
                    () -> assertThat(coupon.getName()).isEqualTo("테스트 쿠폰"),
                    () -> assertThat(coupon.getDescription()).isEqualTo("테스트를 위해 발급된 코드"),
                    () -> assertThat(coupon.getValidStartDate()).isEqualTo(LocalDate.of(2025, 1, 1)),
                    () -> assertThat(coupon.getValidEndDate()).isEqualTo(LocalDate.of(2025, 12, 31)),
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
            Coupon coupon = createCoupon(discountPolicy, "AMOUNT5000", "5000원 할인 쿠폰", "정액 할인 쿠폰",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

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
                    createCoupon(discountPolicy, null, "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TEST_CODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "testcode123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TEST123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE1234567890123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", null, "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", "   ", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(null, "TESTCODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
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
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", null, LocalDate.of(2025, 12, 31))
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 유효일은 필수값 입니다.");
        }

        @DisplayName("유효 종료일이 null이면 예외가 발생한다.")
        @Test
        void createCouponWithNullEndDate_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명", LocalDate.of(2025, 1, 1), null)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("쿠폰 유효일은 필수값 입니다.");
        }

        @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다.")
        @Test
        void createCouponWithInvalidDateRange_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                            LocalDate.of(2025, 12, 31), LocalDate.of(2025, 1, 1))
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("시작일이 종료일보다 늦을 수 없습니다.");
        }
    }

    @DisplayName("쿠폰 유효성 검증")
    @Nested
    class CouponValidation {

        @DisplayName("유효 기간 내의 날짜인지 확인할 수 있다.")
        @Test
        void isValidAt_withinRange_returnsTrue() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when & then
            assertThat(coupon.isValidAt(LocalDate.of(2025, 6, 15))).isTrue();
            assertThat(coupon.isValidAt(LocalDate.of(2025, 1, 1))).isTrue();
            assertThat(coupon.isValidAt(LocalDate.of(2025, 12, 31))).isTrue();
        }

        @DisplayName("유효 기간 밖의 날짜인지 확인할 수 있다.")
        @Test
        void isValidAt_outsideRange_returnsFalse() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when & then
            assertThat(coupon.isValidAt(LocalDate.of(2024, 12, 31))).isFalse();
            assertThat(coupon.isValidAt(LocalDate.of(2026, 1, 1))).isFalse();
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
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

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
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when
            for (int i = 0; i < 100; i++) {
                coupon.increaseIssuanceCount();
            }

            // then
            assertThat(coupon.getCurrentIssuanceCount()).isEqualTo(100);
        }

        @DisplayName("최대 발행 수량에 도달하면 예외가 발생한다.")
        @Test
        void increaseIssuanceCount_exceedLimit_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            coupon.setMaxIssuanceLimit(3);

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

    @DisplayName("쿠폰 상태 관리")
    @Nested
    class CouponStatusManagement {

        @DisplayName("쿠폰을 활성화할 수 있다.")
        @Test
        void activate_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            coupon.deactivate();

            // when
            coupon.activate();

            // then
            assertThat(coupon.isActive()).isTrue();
        }

        @DisplayName("쿠폰을 비활성화할 수 있다.")
        @Test
        void deactivate_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when
            coupon.deactivate();

            // then
            assertThat(coupon.isActive()).isFalse();
        }

        @DisplayName("최대 발행 수량을 설정할 수 있다.")
        @Test
        void setMaxIssuanceLimit_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when
            coupon.setMaxIssuanceLimit(100);

            // then
            assertThat(coupon.getMaxIssuanceLimit()).isEqualTo(100);
        }

        @DisplayName("최대 발행 수량을 null로 설정할 수 있다 (무제한).")
        @Test
        void setMaxIssuanceLimit_unlimited_success() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            coupon.setMaxIssuanceLimit(100);

            // when
            coupon.setMaxIssuanceLimit(null);

            // then
            assertThat(coupon.getMaxIssuanceLimit()).isNull();
        }

        @DisplayName("최대 발행 수량이 음수이면 예외가 발생한다.")
        @Test
        void setMaxIssuanceLimit_negative_throwException() {
            // given
            DiscountPolicy discountPolicy = createValidDiscountPolicy();
            Coupon coupon = createCoupon(discountPolicy, "TESTCODE123", "테스트 쿠폰", "설명",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            // when & then
            CoreException result = assertThrows(CoreException.class, () ->
                    coupon.setMaxIssuanceLimit(-1)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getCustomMessage()).isEqualTo("최대 발행 수량은 0 이상이어야 합니다.");
        }
    }

    private static Coupon createCoupon(
            DiscountPolicy discountPolicy, String code, String name,
            String desc, LocalDate validStartDate, LocalDate validEndDate) {
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