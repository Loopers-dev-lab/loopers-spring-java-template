package com.loopers.infrastructure.coupon;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Component
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    /**
     * Persists the given coupon entity and returns the persisted instance.
     *
     * @param coupon the coupon entity to persist; may be a new or existing entity
     * @return the saved CouponEntity, potentially updated with generated identifiers or persistence-managed state
     */
    @Override
    public CouponEntity save(CouponEntity coupon) {
        return couponJpaRepository.save(coupon);
    }

    /**
     * Retrieve a coupon owned by a specific user that has not been soft-deleted.
     *
     * @param couponId the ID of the coupon to find
     * @param userId the ID of the user who must own the coupon
     * @return an Optional containing the CouponEntity if found and not deleted, otherwise an empty Optional
     */
    @Override
    public Optional<CouponEntity> findByIdAndUserId(Long couponId, Long userId) {
        return couponJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(couponId, userId);
    }
}