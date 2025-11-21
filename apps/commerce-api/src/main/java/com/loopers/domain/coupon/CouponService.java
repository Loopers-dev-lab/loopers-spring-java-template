package com.loopers.domain.coupon;

import java.math.BigDecimal;

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


    /**
     * Create a percentage-based coupon for the given user.
     *
     * @param user    the owner of the coupon
     * @param percent the discount percentage (e.g., 20 for 20%)
     * @return        the persisted CouponEntity representing the created coupon
     */
    @Transactional
    public CouponEntity createPercentCoupon(UserEntity user, int percent) {
        CouponEntity coupon = CouponEntity.createPercentageCoupon(user, percent);
        return couponRepository.save(coupon);
    }

    /**
     * Create a fixed-amount coupon for the given user.
     *
     * @param user the owner of the coupon
     * @param fixedAmount the discount amount for the coupon as a monetary value
     * @return the saved {@link CouponEntity}
     */
    @Transactional
    public CouponEntity createFixedAmountCoupon(UserEntity user, BigDecimal fixedAmount) {
        CouponEntity coupon = CouponEntity.createFixedAmountCoupon(user, fixedAmount);
        return couponRepository.save(coupon);
    }


    /**
     * Retrieve the coupon belonging to the specified user by coupon ID and user ID.
     *
     * @param couponId the ID of the coupon to retrieve
     * @param userId   the ID of the user who must own the coupon
     * @return         the matching CouponEntity
     * @throws IllegalArgumentException if no coupon exists with the given couponId and userId
     */
    @Transactional(readOnly = true)
    public CouponEntity getCouponByIdAndUserId(Long couponId, Long userId) {
        return couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. id: " + couponId));
    }

    /**
     * Marks the given coupon as used and persists the change.
     *
     * @param coupon the coupon to consume
     * @throws IllegalArgumentException if the coupon cannot be consumed due to concurrent modification
     */
    @Transactional
    public void consumeCoupon(CouponEntity coupon) {
        try {
            coupon.use();
            couponRepository.save(coupon);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다. id: " + coupon.getId());
        }
    }

    /**
     * Reverts the given coupon to its prior unused state.
     *
     * @param coupon the coupon to revert to an unused state
     */
    public void revertCoupon(CouponEntity coupon) {
        coupon.revert();
    }
}