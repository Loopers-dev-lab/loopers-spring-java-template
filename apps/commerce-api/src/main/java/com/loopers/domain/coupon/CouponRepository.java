package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
public interface CouponRepository {
    CouponEntity save(CouponEntity any);

    Optional<CouponEntity> findByIdAndUserId(Long couponId, Long userId);

    List<CouponEntity> findByIdsAndUserId(List<Long> couponIds, Long userId);
}
