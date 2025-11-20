package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithLock(@Param("id") Long id);

    List<UserCoupon> findByUserId(String userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.isUsed = false AND uc.deletedAt IS NULL")
    List<UserCoupon> findByUserIdAndIsAvailable(@Param("userId") String userId);
}
