package com.loopers.infrastructure.issuedcoupon;

import com.loopers.domain.issuedcoupon.IssuedCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IssuedCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
