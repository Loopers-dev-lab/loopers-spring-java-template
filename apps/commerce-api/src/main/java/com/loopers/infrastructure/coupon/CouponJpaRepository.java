package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

  @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy WHERE c.id = :id")
  Optional<Coupon> findByIdWithPolicy(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy WHERE c.id = :id")
  Optional<Coupon> findByIdWithPolicyAndLock(@Param("id") Long id);
}
