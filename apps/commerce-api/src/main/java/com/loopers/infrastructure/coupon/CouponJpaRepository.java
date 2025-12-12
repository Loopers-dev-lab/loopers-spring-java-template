package com.loopers.infrastructure.coupon;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 JPA Repository
 */
public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    /**
     * 비관적 락을 사용한 쿠폰 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :id")
    Optional<CouponEntity> findByIdWithLock(@Param("id") Long id);

    /**
     * 사용자 ID로 쿠폰 목록 조회
     */
    List<CouponEntity> findByUserId(String userId);

    /**
     * 사용자 ID와 사용 가능 여부로 쿠폰 목록 조회
     * - 사용되지 않았고, 만료되지 않은 쿠폰
     */
    @Query("SELECT c FROM CouponEntity c WHERE c.userId = :userId AND c.isUsed = false AND c.expiresAt > :now")
    List<CouponEntity> findAvailableCouponsByUserId(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now
    );
}
