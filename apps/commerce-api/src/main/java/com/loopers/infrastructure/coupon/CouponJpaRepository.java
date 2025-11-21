package com.loopers.infrastructure.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.coupon.CouponEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {
    /**
 * Finds a coupon by its id and associated user id that has not been deleted.
 *
 * @param id the coupon's primary key
 * @param userId the id of the user who owns the coupon
 * @return an Optional containing the matching CouponEntity if present, or Optional.empty() if none found
 */
Optional<CouponEntity> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}