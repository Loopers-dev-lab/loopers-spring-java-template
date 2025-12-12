package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 도메인 서비스
 * - 쿠폰 발급, 사용, 검증 등의 비즈니스 로직 처리
 */
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * 쿠폰 발급
     */
    public Coupon issueCoupon(
            String userId,
            String couponName,
            CouponType couponType,
            BigDecimal discountValue,
            LocalDateTime expiresAt
    ) {
        Coupon coupon = Coupon.issue(userId, couponName, couponType, discountValue, expiresAt);
        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 사용
     * - 쿠폰 사용 가능 여부를 검증하고 사용 처리
     */
    public Coupon useCoupon(Long couponId, String userId) {
        // 비관적 락으로 쿠폰 조회
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다"));

        // 쿠폰 소유자 검증
        if (!coupon.isOwnedBy(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰만 사용할 수 있습니다");
        }

        // 쿠폰 사용 처리
        coupon.use();

        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 조회
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다"));
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    public List<Coupon> getUserCoupons(String userId) {
        return couponRepository.findByUserId(userId);
    }

    /**
     * 사용자의 사용 가능한 쿠폰 목록 조회
     */
    public List<Coupon> getAvailableCoupons(String userId) {
        return couponRepository.findAvailableCouponsByUserId(userId);
    }

    /**
     * 쿠폰 할인 금액 계산
     */
    public BigDecimal calculateDiscount(Long couponId, BigDecimal originalAmount) {
        Coupon coupon = getCoupon(couponId);

        if (!coupon.canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다");
        }

        return coupon.calculateDiscount(originalAmount);
    }
}
