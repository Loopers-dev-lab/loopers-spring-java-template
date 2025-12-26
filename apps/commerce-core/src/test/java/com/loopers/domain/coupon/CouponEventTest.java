/*
package com.loopers.domain.coupon;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("쿠폰(Coupon) 이벤트 기반 처리 테스트")
public class CouponEventTest {

    @DisplayName("쿠폰 할인 계산할 때, ")
    @Nested
    class CalculateDiscount {
        @DisplayName("정액 쿠폰 할인을 계산할 수 있다. (사용 처리 없이)")
        @Test
        void should_calculateFixedCouponDiscount_withoutMarkingAsUsed() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.calculateDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(5000);
            assertThat(result.finalPrice().amount()).isEqualTo(5000);
            assertThat(result.couponId()).isNotNull();
            // 사용 처리되지 않았는지 확인
            assertThat(coupon.isUsed()).isFalse();
        }

        @DisplayName("정률 쿠폰 할인을 계산할 수 있다. (사용 처리 없이)")
        @Test
        void should_calculatePercentageCouponDiscount_withoutMarkingAsUsed() {
            // arrange
            Coupon coupon = Coupon.issuePercentage(1L, 20);
            Price originalPrice = new Price(10000);

            // act
            DiscountResult result = coupon.calculateDiscount(originalPrice);

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(2000);
            assertThat(result.finalPrice().amount()).isEqualTo(8000);
            assertThat(result.couponId()).isNotNull();
            // 사용 처리되지 않았는지 확인
            assertThat(coupon.isUsed()).isFalse();
        }

        @DisplayName("이미 사용된 쿠폰의 할인을 계산할 경우, 예외가 발생한다.")
        @Test
        void should_throwException_when_calculatingDiscountForUsedCoupon() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            Price originalPrice = new Price(10000);
            coupon.markAsUsed(); // 먼저 사용 처리

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> coupon.calculateDiscount(originalPrice));
            assertThat(exception.getMessage()).isEqualTo("이미 사용된 쿠폰입니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰 사용 처리할 때, ")
    @Nested
    class MarkAsUsed {
        @DisplayName("정상적으로 쿠폰을 사용 처리할 수 있다.")
        @Test
        void should_markCouponAsUsed_when_validCoupon() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            assertThat(coupon.isUsed()).isFalse();

            // act
            coupon.markAsUsed();

            // assert
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("이미 사용된 쿠폰을 다시 사용 처리할 경우, 예외가 발생한다.")
        @Test
        void should_throwException_when_markingAlreadyUsedCoupon() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            coupon.markAsUsed(); // 첫 번째 사용 처리

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> coupon.markAsUsed());
            assertThat(exception.getMessage()).isEqualTo("이미 사용된 쿠폰입니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("이벤트 기반 처리 플로우 테스트")
    @Nested
    class EventBasedFlow {
        @DisplayName("할인 계산 후 사용 처리를 분리할 수 있다.")
        @Test
        void should_separateCalculationAndUsage() {
            // arrange
            Coupon coupon = Coupon.issueFixed(1L, 5000);
            Price originalPrice = new Price(10000);

            // act - 1단계: 할인 계산만 수행
            DiscountResult result = coupon.calculateDiscount(originalPrice);
            assertThat(coupon.isUsed()).isFalse(); // 아직 사용 처리 안 됨

            // act - 2단계: 사용 처리
            coupon.markAsUsed();

            // assert
            assertThat(result.originalPrice().amount()).isEqualTo(10000);
            assertThat(result.discountAmount().amount()).isEqualTo(5000);
            assertThat(result.finalPrice().amount()).isEqualTo(5000);
            assertThat(coupon.isUsed()).isTrue(); // 사용 처리 완료
        }
    }
}

*/




