package com.loopers.domain.coupon;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("쿠폰(Coupon) Entity 테스트")
public class CouponTest {

    @DisplayName("정률 쿠폰을 발급할 때, ")
    @Nested
    class IssuePercentage {
        @DisplayName("정상적인 userId와 할인율로 쿠폰을 발급할 수 있다. (Happy Path)")
        @Test
        void should_issuePercentageCoupon_when_validInputs() {
            // arrange
            Long userId = 1L;
            Integer discountPercent = 20;

            // act
            Coupon coupon = Coupon.issuePercentage(userId, discountPercent);

            // assert
            assertThat(coupon.getUserId()).isEqualTo(1L);
            assertThat(coupon.getCouponType()).isEqualTo(CouponType.PERCENTAGE);
            assertThat(coupon.getDiscountValue()).isEqualTo(20);
            assertThat(coupon.isUsed()).isFalse();
        }

        @DisplayName("null userId로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userIdIsNull() {
            // arrange
            Long userId = null;
            Integer discountPercent = 20;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issuePercentage(userId, discountPercent));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("0 이하의 userId로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(longs = {0, -1, -100})
        void should_throwException_when_userIdIsInvalid(Long userId) {
            // arrange
            Integer discountPercent = 20;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issuePercentage(userId, discountPercent));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null 할인율로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_discountPercentIsNull() {
            // arrange
            Long userId = 1L;
            Integer discountPercent = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issuePercentage(userId, discountPercent));
            assertThat(exception.getMessage()).isEqualTo("할인 값은 0보다 커야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("0 이하의 할인율로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void should_throwException_when_discountPercentIsInvalid(Integer discountPercent) {
            // arrange
            Long userId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issuePercentage(userId, discountPercent));
            assertThat(exception.getMessage()).isEqualTo("할인 값은 0보다 커야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("100%를 초과하는 할인율로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {101, 200, 1000})
        void should_throwException_when_discountPercentExceeds100(Integer discountPercent) {
            // arrange
            Long userId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issuePercentage(userId, discountPercent));
            assertThat(exception.getMessage()).isEqualTo("정률 할인은 100%를 초과할 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("100% 할인율로 쿠폰을 발급할 수 있다. (Edge Case)")
        @Test
        void should_issuePercentageCoupon_when_discountPercentIs100() {
            // arrange
            Long userId = 1L;
            Integer discountPercent = 100;

            // act
            Coupon coupon = Coupon.issuePercentage(userId, discountPercent);

            // assert
            assertThat(coupon.getCouponType()).isEqualTo(CouponType.PERCENTAGE);
            assertThat(coupon.getDiscountValue()).isEqualTo(100);
        }
    }

    @DisplayName("정액 쿠폰을 발급할 때, ")
    @Nested
    class IssueFixed {
        @DisplayName("정상적인 userId와 할인 금액으로 쿠폰을 발급할 수 있다. (Happy Path)")
        @Test
        void should_issueFixedCoupon_when_validInputs() {
            // arrange
            Long userId = 1L;
            Integer discountAmount = 5000;

            // act
            Coupon coupon = Coupon.issueFixed(userId, discountAmount);

            // assert
            assertThat(coupon.getUserId()).isEqualTo(1L);
            assertThat(coupon.getCouponType()).isEqualTo(CouponType.FIXED);
            assertThat(coupon.getDiscountValue()).isEqualTo(5000);
            assertThat(coupon.isUsed()).isFalse();
        }

        @DisplayName("null userId로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userIdIsNull() {
            // arrange
            Long userId = null;
            Integer discountAmount = 5000;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issueFixed(userId, discountAmount));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("0 이하의 userId로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(longs = {0, -1, -100})
        void should_throwException_when_userIdIsInvalid(Long userId) {
            // arrange
            Integer discountAmount = 5000;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issueFixed(userId, discountAmount));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null 할인 금액으로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_discountAmountIsNull() {
            // arrange
            Long userId = 1L;
            Integer discountAmount = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issueFixed(userId, discountAmount));
            assertThat(exception.getMessage()).isEqualTo("할인 값은 0보다 커야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("0 이하의 할인 금액으로 쿠폰을 발급할 경우, 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void should_throwException_when_discountAmountIsInvalid(Integer discountAmount) {
            // arrange
            Long userId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Coupon.issueFixed(userId, discountAmount));
            assertThat(exception.getMessage()).isEqualTo("할인 값은 0보다 커야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰 할인을 적용할 때, ")
    @Nested
    class ApplyDiscount {
        @DisplayName("정액 쿠폰을 정상적으로 적용할 수 있다. (Happy Path)")
        @Test
        void should_applyFixedCoupon_when_validPrice() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(5000);
            assertThat(result.finalPrice().amount()).isEqualTo(5000);
            assertThat(result.couponId()).isNotNull();
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("정액 쿠폰 할인 금액이 원가보다 큰 경우, 원가만큼만 할인된다. (Edge Case)")
        @Test
        void should_applyFixedCoupon_when_discountExceedsOriginalPrice() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 15000); // 할인 금액이 원가보다 큼
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(10000); // 원가만큼만 할인
            assertThat(result.finalPrice().amount()).isEqualTo(0);
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("정률 쿠폰을 정상적으로 적용할 수 있다. (Happy Path)")
        @Test
        void should_applyPercentageCoupon_when_validPrice() {
            // arrange
            Coupon coupon = Coupon.issuePercentage(1L, 20); // 20% 할인
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(2000); // 10000 * 20 / 100
            assertThat(result.finalPrice().amount()).isEqualTo(8000);
            assertThat(result.couponId()).isNotNull();
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("정률 쿠폰 100% 할인을 적용할 수 있다. (Edge Case)")
        @Test
        void should_applyPercentageCoupon_when_discountIs100() {
            // arrange
            Coupon coupon = Coupon.issuePercentage(1L, 100); // 100% 할인
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(10000);
            assertThat(result.finalPrice().amount()).isEqualTo(0);
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("이미 사용된 쿠폰을 적용할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_couponAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            Price originalPrice = new Price(10000);
            coupon.applyDiscount(originalPrice); // 첫 번째 사용

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> coupon.applyDiscount(originalPrice));
            assertThat(exception.getMessage()).isEqualTo("이미 사용된 쿠폰입니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정률 쿠폰 할인 계산이 소수점 이하인 경우, 정수로 내림 처리된다. (Edge Case)")
        @Test
        void should_applyPercentageCoupon_when_discountHasDecimal() {
            // arrange
            Coupon coupon = Coupon.issuePercentage(1L, 33); // 33% 할인
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            // 10000 * 33 / 100 = 3300 (정수 나눗셈)
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(3300);
            assertThat(result.finalPrice().amount()).isEqualTo(6700);
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("정률 쿠폰으로 큰 금액에 할인을 적용할 수 있다. (Edge Case)")
        @Test
        void should_applyPercentageCoupon_when_largeAmount() {
            // arrange
            Coupon coupon = Coupon.issuePercentage(1L, 10); // 10% 할인
            Price originalPrice = new Price(1000000); // 100만원

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(1000000);
            assertThat(result.discountAmount().amount()).isEqualTo(100000); // 1000000 * 10 / 100
            assertThat(result.finalPrice().amount()).isEqualTo(900000);
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("정액 쿠폰으로 작은 금액에 할인을 적용할 수 있다. (Edge Case)")
        @Test
        void should_applyFixedCoupon_when_smallAmount() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 100); // 100원 할인
            Price originalPrice = new Price(500); // 500원

            // act
            DiscountResult result = coupon.applyDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(500);
            assertThat(result.discountAmount().amount()).isEqualTo(100); // 원가만큼만 할인
            assertThat(result.finalPrice().amount()).isEqualTo(400);
            assertThat(coupon.isUsed()).isTrue();
        }
    }
}

