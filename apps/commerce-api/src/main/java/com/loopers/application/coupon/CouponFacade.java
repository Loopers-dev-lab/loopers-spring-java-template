package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 애플리케이션 파사드
 * - Application Layer에서 트랜잭션 관리
 */
@Service
public class CouponFacade {

    private final CouponService couponService;

    public CouponFacade(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 쿠폰 발급
     */
    @Transactional
    public Coupon issueCoupon(
            String userId,
            String couponName,
            CouponType couponType,
            BigDecimal discountValue,
            LocalDateTime expiresAt
    ) {
        return couponService.issueCoupon(userId, couponName, couponType, discountValue, expiresAt);
    }

    /**
     * 쿠폰 사용
     */
    @Transactional
    public Coupon useCoupon(Long couponId, String userId) {
        return couponService.useCoupon(couponId, userId);
    }

    /**
     * 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponService.getCoupon(couponId);
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Coupon> getUserCoupons(String userId) {
        return couponService.getUserCoupons(userId);
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Coupon> getAvailableCoupons(String userId) {
        return couponService.getAvailableCoupons(userId);
    }

    /**
     * 쿠폰 할인 금액 계산
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Long couponId, BigDecimal originalAmount) {
        return couponService.calculateDiscount(couponId, originalAmount);
    }
}
