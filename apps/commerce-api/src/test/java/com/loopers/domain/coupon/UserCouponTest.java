package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserCoupon 도메인 테스트")
class UserCouponTest {

    private Coupon createTestCoupon() {
        return Coupon.builder()
            .name("테스트 쿠폰")
            .type(CouponType.FIXED_AMOUNT)
            .discountValue(BigDecimal.valueOf(5000))
            .description("테스트용 쿠폰")
            .build();
    }

    @DisplayName("사용자 쿠폰 생성 시")
    @Nested
    class CreateUserCoupon {

        @DisplayName("정상적으로 사용자 쿠폰을 생성할 수 있다")
        @Test
        void createUserCoupon_success() {
            // given
            String userId = "user123";
            Coupon coupon = createTestCoupon();

            // when
            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();

            // then
            assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(userId),
                () -> assertThat(userCoupon.getCoupon()).isEqualTo(coupon),
                () -> assertThat(userCoupon.isUsed()).isFalse(),
                () -> assertThat(userCoupon.getUsedAt()).isNull()
            );
        }

        @DisplayName("사용자 ID가 null이면 예외가 발생한다")
        @Test
        void createUserCoupon_withNullUserId_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                UserCoupon.builder()
                    .userId(null)
                    .coupon(createTestCoupon())
                    .build()
            );

            assertThat(exception.getMessage()).contains("사용자 ID는 필수입니다");
        }

        @DisplayName("사용자 ID가 빈 문자열이면 예외가 발생한다")
        @Test
        void createUserCoupon_withBlankUserId_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                UserCoupon.builder()
                    .userId("   ")
                    .coupon(createTestCoupon())
                    .build()
            );

            assertThat(exception.getMessage()).contains("사용자 ID는 필수입니다");
        }

        @DisplayName("쿠폰이 null이면 예외가 발생한다")
        @Test
        void createUserCoupon_withNullCoupon_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                UserCoupon.builder()
                    .userId("user123")
                    .coupon(null)
                    .build()
            );

            assertThat(exception.getMessage()).contains("쿠폰은 필수입니다");
        }
    }

    @DisplayName("쿠폰 사용 시")
    @Nested
    class UseCoupon {

        @DisplayName("쿠폰을 정상적으로 사용할 수 있다")
        @Test
        void useCoupon_success() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();

            // when
            userCoupon.use();

            // then
            assertAll(
                () -> assertThat(userCoupon.isUsed()).isTrue(),
                () -> assertThat(userCoupon.getUsedAt()).isNotNull(),
                () -> assertThat(userCoupon.isAvailable()).isFalse()
            );
        }

        @DisplayName("이미 사용한 쿠폰은 다시 사용할 수 없다")
        @Test
        void useCoupon_alreadyUsed_throwsException() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();
            userCoupon.use();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                userCoupon.use()
            );

            assertThat(exception.getMessage()).contains("이미 사용된 쿠폰입니다");
        }

        @DisplayName("삭제된 쿠폰은 사용할 수 없다")
        @Test
        void useCoupon_deleted_throwsException() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();
            userCoupon.delete(); // soft delete

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                userCoupon.use()
            );

            assertThat(exception.getMessage()).contains("삭제된 쿠폰입니다");
        }
    }

    @DisplayName("쿠폰 사용 가능 여부 확인 시")
    @Nested
    class IsAvailable {

        @DisplayName("사용하지 않은 쿠폰은 사용 가능하다")
        @Test
        void isAvailable_notUsed_returnsTrue() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();

            // when & then
            assertThat(userCoupon.isAvailable()).isTrue();
        }

        @DisplayName("사용한 쿠폰은 사용 불가능하다")
        @Test
        void isAvailable_used_returnsFalse() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();
            userCoupon.use();

            // when & then
            assertThat(userCoupon.isAvailable()).isFalse();
        }

        @DisplayName("삭제된 쿠폰은 사용 불가능하다")
        @Test
        void isAvailable_deleted_returnsFalse() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();
            userCoupon.delete(); // soft delete

            // when & then
            assertThat(userCoupon.isAvailable()).isFalse();
        }

        @DisplayName("사용하고 삭제된 쿠폰은 사용 불가능하다")
        @Test
        void isAvailable_usedAndDeleted_returnsFalse() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                .userId("user123")
                .coupon(createTestCoupon())
                .build();
            userCoupon.use();
            userCoupon.delete();

            // when & then
            assertThat(userCoupon.isAvailable()).isFalse();
        }
    }
}
