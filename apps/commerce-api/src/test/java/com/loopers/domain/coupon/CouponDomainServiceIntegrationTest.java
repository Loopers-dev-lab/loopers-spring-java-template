package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class CouponDomainServiceIntegrationTest {

    @Autowired
    private CouponDomainService couponDomainService;

    @Autowired
    private CouponRepository couponRepository;

    static final String USER_ID = "user123";

    @Nested
    @DisplayName("쿠폰 발급")
    class IssueCouponTest {

        @Test
        @DisplayName("쿠폰을 발급하면 ISSUED 상태로 저장된다")
        void test1() {
            Coupon coupon = couponDomainService.issueCoupon(USER_ID, "신규 가입 쿠폰", 1000L);

            assertThat(coupon.getId()).isNotNull();
            assertThat(coupon.getUserId()).isEqualTo(USER_ID);
            assertThat(coupon.getName()).isEqualTo("신규 가입 쿠폰");
            assertThat(coupon.getDiscountAmount()).isEqualTo(1000L);
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ISSUED);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용")
    class UseCouponTest {

        @Test
        @DisplayName("쿠폰 사용 시 USED 상태로 변경된다")
        void test1() {
            Coupon coupon = couponDomainService.issueCoupon(USER_ID, "신규 가입 쿠폰", 1000L);

            couponDomainService.useCoupon(coupon.getId());

            Coupon usedCoupon = couponDomainService.getCoupon(coupon.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(usedCoupon.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 사용 시 예외 발생")
        void test2() {
            assertThatThrownBy(() -> couponDomainService.useCoupon(999L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("쿠폰을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("사용 가능한 쿠폰 조회")
    class GetUsableCouponsTest {

        @Test
        @DisplayName("ISSUED 상태의 쿠폰만 조회된다")
        void test1() {
            couponDomainService.issueCoupon(USER_ID, "쿠폰1", 1000L);
            Coupon usedCoupon = couponDomainService.issueCoupon(USER_ID, "쿠폰2", 2000L);
            couponDomainService.useCoupon(usedCoupon.getId());
            couponDomainService.issueCoupon(USER_ID, "쿠폰3", 3000L);

            List<Coupon> usableCoupons = couponDomainService.getUsableCoupons(USER_ID);

            assertThat(usableCoupons).hasSize(2);
            assertThat(usableCoupons).allMatch(c -> c.getStatus() == CouponStatus.ISSUED);
        }
    }

    @Nested
    @DisplayName("쿠폰 검증")
    class ValidateAndGetCouponTest {

        @Test
        @DisplayName("소유자가 아닌 사용자가 쿠폰 사용 시 예외 발생")
        void test1() {
            Coupon coupon = couponDomainService.issueCoupon(USER_ID, "쿠폰", 1000L);

            assertThatThrownBy(() -> couponDomainService.validateAndGetCoupon("otherUser", coupon.getId()))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("해당 쿠폰의 소유자가 아닙니다");
        }

        @Test
        @DisplayName("이미 사용된 쿠폰 검증 시 예외 발생")
        void test2() {
            Coupon coupon = couponDomainService.issueCoupon(USER_ID, "쿠폰", 1000L);
            couponDomainService.useCoupon(coupon.getId());

            assertThatThrownBy(() -> couponDomainService.validateAndGetCoupon(USER_ID, coupon.getId()))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이미 사용된 쿠폰입니다");
        }
    }
}
