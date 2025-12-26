package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByIdAndUserId(Long couponId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId AND c.userId = :userId")
    Optional<Coupon> findByIdAndUserIdForUpdate(Long couponId, Long userId);
}
