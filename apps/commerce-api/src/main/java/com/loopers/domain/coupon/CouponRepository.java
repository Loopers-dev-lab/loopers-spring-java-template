package com.loopers.domain.coupon;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface CouponRepository {
    /**
 * Persist the given coupon entity and return the resulting entity.
 *
 * @param any the coupon entity to save or update
 * @return the saved {@code CouponEntity}
 */
CouponEntity save(CouponEntity any);

    /**
 * Retrieves a coupon by its identifier and the owner's user identifier.
 *
 * @param couponId the coupon's identifier
 * @param userId the identifier of the user who owns the coupon
 * @return an Optional containing the CouponEntity if found, or an empty Optional if not found
 */
Optional<CouponEntity> findByIdAndUserId(Long couponId, Long userId);
}