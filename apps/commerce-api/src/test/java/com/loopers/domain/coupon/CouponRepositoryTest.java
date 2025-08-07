package com.loopers.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@Transactional
@DisplayName("CouponRepository 통합 테스트")
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("쿠폰을 저장할 수 있다")
    void saveCoupon() {
        // arrange
        CouponModel coupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));

        // act
        CouponModel savedCoupon = couponRepository.save(coupon);

        // assert
        assertThat(savedCoupon.getId()).isNotNull();
        assertThat(savedCoupon.getUserId().getValue()).isEqualTo(1L);
        assertThat(savedCoupon.getValue().getValue()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("ID로 쿠폰을 조회할 수 있다")
    void findById() {
        // arrange
        CouponModel coupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        CouponModel savedCoupon = couponRepository.save(coupon);

        // act
        Optional<CouponModel> foundCoupon = couponRepository.findById(savedCoupon.getId());

        // assert
        assertThat(foundCoupon).isPresent();
        assertThat(foundCoupon.get().getValue().getValue()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 목록을 조회할 수 있다")
    void findByUserId() {
        // arrange
        Long userId = 1L;
        CouponModel coupon1 = CouponModel.createFixed(userId, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        CouponModel coupon2 = CouponModel.createRate(userId, new BigDecimal("0.1"), LocalDateTime.now().plusDays(30));
        CouponModel coupon3 = CouponModel.createFixed(2L, new BigDecimal("3000"), LocalDateTime.now().plusDays(30)); // 다른 사용자

        couponRepository.save(coupon1);
        couponRepository.save(coupon2);
        couponRepository.save(coupon3);

        // act
        List<CouponModel> userCoupons = couponRepository.findByUserId(userId);

        // assert
        assertThat(userCoupons).hasSize(2);
        assertThat(userCoupons).allMatch(coupon -> coupon.belongsToUser(userId));
    }

    @Test
    @DisplayName("사용자의 사용 가능한 쿠폰 목록을 조회할 수 있다")
    void findUsableCouponsByUserId() {
        // arrange
        Long userId = 1L;
        CouponModel usableCoupon = CouponModel.createFixed(userId, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        CouponModel usedCoupon = CouponModel.createFixed(userId, new BigDecimal("3000"), LocalDateTime.now().plusDays(30));

        couponRepository.save(usableCoupon);
        CouponModel savedUsedCoupon = couponRepository.save(usedCoupon);
        savedUsedCoupon.use(100L);
        couponRepository.save(savedUsedCoupon);

        // act
        List<CouponModel> usableCoupons = couponRepository.findUsableCouponsByUserId(userId);

        // assert
        assertThat(usableCoupons).hasSize(1);
        assertThat(usableCoupons.get(0).canUse()).isTrue();
    }

    @Test
    @DisplayName("주문 ID로 사용된 쿠폰을 조회할 수 있다")
    void findByOrderId() {
        // arrange
        Long orderId = 100L;
        CouponModel coupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        CouponModel savedCoupon = couponRepository.save(coupon);
        savedCoupon.use(orderId);
        couponRepository.save(savedCoupon);

        // act
        List<CouponModel> usedCoupons = couponRepository.findByOrderId(orderId);

        // assert
        assertThat(usedCoupons).hasSize(1);
        assertThat(usedCoupons.get(0).getOrderId().getValue()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("쿠폰 ID 존재 여부를 확인할 수 있다")
    void existsById() {
        // arrange
        CouponModel coupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        CouponModel savedCoupon = couponRepository.save(coupon);

        // act & assert
        assertThat(couponRepository.existsById(savedCoupon.getId())).isTrue();
        assertThat(couponRepository.existsById(9999L)).isFalse();
    }
}
