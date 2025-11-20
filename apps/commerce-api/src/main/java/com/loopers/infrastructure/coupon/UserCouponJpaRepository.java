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
     * Finds the UserCoupon that matches the given user ID and coupon code.
     *
     * @param userId     the ID of the user
     * @param couponCode the coupon code
     * @return an Optional containing the matching UserCoupon, or empty if none is found
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND c.code = :couponCode")
    Optional<UserCoupon> findByUserIdAndCouponCode(@Param("userId") Long userId, @Param("couponCode") String couponCode);

    /**
     * Retrieves the UserCoupon for the given user ID and coupon code, intended for update under optimistic locking.
     *
     * @param userId the ID of the user
     * @param couponCode the coupon code
     * @return an Optional containing the matching UserCoupon if present, or Optional.empty() otherwise
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND c.code = :couponCode")
    Optional<UserCoupon> findByUserIdAndCouponCodeForUpdate(@Param("userId") Long userId, @Param("couponCode") String couponCode);
}
