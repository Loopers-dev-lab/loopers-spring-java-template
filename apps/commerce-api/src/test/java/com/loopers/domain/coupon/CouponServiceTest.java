package com.loopers.domain.coupon;

import com.loopers.domain.coupon.fixture.CouponFixture;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CouponService 테스트")
class CouponServiceTest {

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService();
    }

    @Test
    @DisplayName("사용자에게 속한 쿠폰을 필터링할 수 있다")
    void filterCouponsByUser() {
        // Arrange
        Long targetUserId = 1L;
        List<CouponModel> coupons = List.of(
                CouponFixture.createFixedCouponWithUserId(1L),
                CouponFixture.createFixedCouponWithUserId(2L),
                CouponFixture.createRateCouponWithUserId(1L)
        );

        // Act
        List<CouponModel> userCoupons = couponService.filterCouponsByUser(coupons, targetUserId);

        // Assert
        assertThat(userCoupons).hasSize(2);
        assertThat(userCoupons).allMatch(coupon -> coupon.belongsToUser(targetUserId));
    }

    @Test
    @DisplayName("사용 가능한 쿠폰만 필터링할 수 있다")
    void filterUsableCoupons() {
        // Arrange
        CouponModel usableCoupon = CouponFixture.createFixedCoupon();
        CouponModel usedCoupon = CouponFixture.createUsedFixedCoupon();

        List<CouponModel> coupons = List.of(usableCoupon, usedCoupon);

        // Act
        List<CouponModel> usableCoupons = couponService.filterUsableCoupons(coupons);

        // Assert
        assertThat(usableCoupons).hasSize(1);
        assertThat(usableCoupons.get(0)).isEqualTo(usableCoupon);
    }

    @Test
    @DisplayName("최적의 쿠폰을 선택할 수 있다")
    void selectBestCoupon() {
        // Arrange
        BigDecimal orderAmount = new BigDecimal("10000");
        List<CouponModel> coupons = List.of(
                CouponFixture.createFixedCouponWithAmount(new BigDecimal("2000")),
                CouponFixture.createFixedCouponWithAmount(new BigDecimal("5000")),
                CouponFixture.createRateCouponWithRate(new BigDecimal("0.15")) // 15% = 1500원
        );

        // Act
        CouponModel bestCoupon = couponService.selectBestCoupon(coupons, orderAmount);

        // Assert
        assertThat(bestCoupon.calculateDiscountAmount(orderAmount)).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("사용 가능한 쿠폰이 없으면 예외가 발생한다")
    void throwExceptionWhenNoCouponsAvailable() {
        // Arrange
        List<CouponModel> emptyCoupons = List.of();
        BigDecimal orderAmount = new BigDecimal("10000");

        // Act & Assert
        assertThatThrownBy(() -> couponService.selectBestCoupon(emptyCoupons, orderAmount))
                .isInstanceOf(CoreException.class)
                .hasMessage("사용할 수 있는 쿠폰이 없습니다.");
    }

    @Test
    @DisplayName("사용자의 사용 가능한 쿠폰 중에서 최적의 쿠폰을 찾을 수 있다")
    void findBestCouponForUser() {
        // Arrange
        Long userId = 1L;
        BigDecimal orderAmount = new BigDecimal("20000");
        
        List<CouponModel> allCoupons = List.of(
                CouponFixture.createFixedCouponWithUserIdAndAmount(1L, new BigDecimal("3000")),
                CouponFixture.createFixedCouponWithUserIdAndAmount(2L, new BigDecimal("5000")), // 다른 사용자
                CouponFixture.createRateCouponWithUserIdAndRate(1L, new BigDecimal("0.2")) // 20% = 4000원
        );

        // Act
        CouponModel bestCoupon = couponService.findBestCouponForUser(allCoupons, userId, orderAmount);

        // Assert
        assertThat(bestCoupon.calculateDiscountAmount(orderAmount)).isEqualTo(new BigDecimal("4000"));
        assertThat(bestCoupon.belongsToUser(userId)).isTrue();
    }

    @Test
    @DisplayName("할인 금액을 계산할 수 있다")
    void calculateTotalDiscount() {
        // Arrange
        List<CouponModel> coupons = List.of(
                CouponFixture.createFixedCoupon(),
                CouponFixture.createRateCoupon()
        );
        BigDecimal orderAmount = new BigDecimal("10000");

        // Act
        BigDecimal totalDiscount = couponService.calculateTotalDiscount(coupons, orderAmount);

        // Assert - 5000원 + 1000원 = 6000원
        assertThat(totalDiscount).isEqualTo(new BigDecimal("6000"));
    }
}