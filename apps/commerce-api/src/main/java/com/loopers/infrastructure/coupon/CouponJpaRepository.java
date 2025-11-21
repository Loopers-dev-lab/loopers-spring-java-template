package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId AND c.userId = :userId")
    Optional<Coupon> findByIdAndUserIdWithLock(@Param("couponId") Long couponId, @Param("userId") Long userId);

}

