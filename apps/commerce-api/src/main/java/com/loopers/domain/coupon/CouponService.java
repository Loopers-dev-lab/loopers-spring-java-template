package com.loopers.domain.coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */

@RequiredArgsConstructor
@Component
public class CouponService {
    private final CouponRepository couponRepository;


    @Transactional
    public CouponEntity createPercentCoupon(UserEntity user, int percent) {
        CouponEntity coupon = CouponEntity.createPercentageCoupon(user, percent);
        return couponRepository.save(coupon);
    }

    @Transactional
    public CouponEntity createFixedAmountCoupon(UserEntity user, BigDecimal fixedAmount) {
        CouponEntity coupon = CouponEntity.createFixedAmountCoupon(user, fixedAmount);
        return couponRepository.save(coupon);
    }


    @Transactional(readOnly = true)
    public CouponEntity getCouponByIdAndUserId(Long couponId, Long userId) {
        return couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. id: " + couponId));
    }

    @Transactional
    public void consumeCoupon(CouponEntity coupon) {
        try {
            coupon.use();
            couponRepository.save(coupon);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다. id: " + coupon.getId());
        }
    }

    @Transactional
    public void revertCoupon(CouponEntity coupon) {
        coupon.revert();
        couponRepository.save(coupon);
    }

    public List<CouponEntity> getCouponByIdsAndUserId(List<Long> couponIds, Long userId) {
        return couponRepository.findByIdsAndUserId(couponIds, userId);
    }

    /**
     * Consumes a list of coupons by marking them as used for a specific order.
     *
     * @param coupons the list of {@code CouponEntity} objects to be consumed
     * @param orderId the ID of the order for which the coupons are being consumed
     */
    @Transactional
    public void consumeCoupons(List<CouponEntity> coupons, Long orderId) {
        coupons.stream()
                .filter(Objects::nonNull)
                .forEach(coupon -> {
                    coupon.useForOrder(orderId);
                    couponRepository.save(coupon);
                });
    }
}
