package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UserCoupon 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
    /**
     * 사용자 ID와 쿠폰 코드로 사용자 쿠폰을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param couponCode 쿠폰 코드
     * @return 조회된 사용자 쿠폰을 담은 Optional
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND c.code = :couponCode")
    Optional<UserCoupon> findByUserIdAndCouponCode(@Param("userId") Long userId, @Param("couponCode") String couponCode);

    /**
     * 사용자 ID와 쿠폰 코드로 사용자 쿠폰을 조회합니다.
     * <p>
     * Optimistic Lock을 사용하여 동시성 제어를 보장합니다.
     * UserCoupon 엔티티의 @Version 필드를 통해 자동으로 낙관적 락이 적용됩니다.
     * </p>
     * <p>
     * <b>Lock 전략:</b>
     * <ul>
     *   <li><b>OPTIMISTIC_LOCK 선택 근거:</b> 쿠폰 사용 시 Lost Update 방지, Hot Spot 대응</li>
     *   <li><b>@Version 필드:</b> 엔티티에 version 필드가 있어 자동으로 낙관적 락 적용</li>
     *   <li><b>동시 사용 시:</b> 한 명만 성공하고 나머지는 OptimisticLockException 발생</li>
     * </ul>
     * </p>
     * <p>
     * <b>동작 원리:</b>
     * <ol>
     *   <li>일반 조회로 UserCoupon 엔티티 로드 (version 포함)</li>
     *   <li>쿠폰 사용 처리 (isUsed = true)</li>
     *   <li>저장 시 version 체크 → 다른 트랜잭션이 먼저 수정했다면 OptimisticLockException 발생</li>
     *   <li>예외 발생 시 쿠폰 사용 실패 처리</li>
     * </ol>
     * </p>
     *
     * @param userId 사용자 ID
     * @param couponCode 쿠폰 코드
     * @return 조회된 사용자 쿠폰을 담은 Optional
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND c.code = :couponCode")
    Optional<UserCoupon> findByUserIdAndCouponCodeForUpdate(@Param("userId") Long userId, @Param("couponCode") String couponCode);
}

