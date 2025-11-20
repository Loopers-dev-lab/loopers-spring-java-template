package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * UserCouponRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 UserCoupon 엔티티의
 * 영속성 작업을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class UserCouponRepositoryImpl implements UserCouponRepository {
    private final UserCouponJpaRepository userCouponJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }

    /**
     * Finds a user's coupon by user ID and coupon code.
     *
     * @param userId     the ID of the user to search coupons for
     * @param couponCode the coupon code to match
     * @return an Optional containing the matching UserCoupon if found, `Optional.empty()` otherwise
     */
    @Override
    public Optional<UserCoupon> findByUserIdAndCouponCode(Long userId, String couponCode) {
        return userCouponJpaRepository.findByUserIdAndCouponCode(userId, couponCode);
    }

    /**
         * Finds a user's coupon by user ID and coupon code while acquiring a row-level lock for update.
         *
         * @param userId     the ID of the user who owns the coupon
         * @param couponCode the coupon code to match
         * @return an Optional containing the matching UserCoupon if present, `Optional.empty()` otherwise
         */
    @Override
    public Optional<UserCoupon> findByUserIdAndCouponCodeForUpdate(Long userId, String couponCode) {
        return userCouponJpaRepository.findByUserIdAndCouponCodeForUpdate(userId, couponCode);
    }
}
