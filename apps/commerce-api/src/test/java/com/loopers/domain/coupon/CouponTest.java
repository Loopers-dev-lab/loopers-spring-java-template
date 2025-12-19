package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CouponTest {

    static final String USER_ID = "user123";

    @Nested
    @DisplayName("Coupon 생성")
    class CreateTest {

        @Test
        @DisplayName("쿠폰 생성 시 ISSUED 상태이다")
        void test1() {
            Coupon coupon = Coupon.create(USER_ID, "신규 가입 쿠폰", 1000L);

            assertThat(coupon)
                    .extracting(
                            Coupon::getUserId,
                            Coupon::getName,
                            Coupon::getDiscountAmount,
                            Coupon::getStatus,
                            Coupon::getUsedAt
                    )
                    .containsExactly(USER_ID, "신규 가입 쿠폰", 1000L, CouponStatus.ISSUED, null);
        }

        @Test
        @DisplayName("할인 금액이 0 이하면 예외 발생")
        void test2() {
            assertThatThrownBy(() -> Coupon.create(USER_ID, "잘못된 쿠폰", 0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("할인 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("할인 금액이 음수면 예외 발생")
        void test3() {
            assertThatThrownBy(() -> Coupon.create(USER_ID, "잘못된 쿠폰", -1000L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("할인 금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("Coupon 사용")
    class UseTest {

        @Test
        @DisplayName("ISSUED 상태에서 use 호출 시 USED 상태로 전환된다")
        void test1() {
            Coupon coupon = Coupon.create(USER_ID, "신규 가입 쿠폰", 1000L);

            coupon.use();

            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(coupon.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 USED 상태에서 use 재호출 시 멱등하게 동작한다")
        void test2() {
            Coupon coupon = Coupon.create(USER_ID, "신규 가입 쿠폰", 1000L);
            coupon.use();

            coupon.use();

            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
        }
    }

    @Nested
    @DisplayName("Coupon 사용 가능 여부 확인")
    class IsUsableTest {

        @Test
        @DisplayName("ISSUED 상태면 사용 가능")
        void test1() {
            Coupon coupon = Coupon.create(USER_ID, "신규 가입 쿠폰", 1000L);

            assertThat(coupon.isUsable()).isTrue();
        }

        @Test
        @DisplayName("USED 상태면 사용 불가")
        void test2() {
            Coupon coupon = Coupon.create(USER_ID, "신규 가입 쿠폰", 1000L);
            coupon.use();

            assertThat(coupon.isUsable()).isFalse();
        }
    }
}
