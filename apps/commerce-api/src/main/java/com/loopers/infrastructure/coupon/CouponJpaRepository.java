package com.loopers.infrastructure.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.coupon.CouponEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {
    Optional<CouponEntity> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
