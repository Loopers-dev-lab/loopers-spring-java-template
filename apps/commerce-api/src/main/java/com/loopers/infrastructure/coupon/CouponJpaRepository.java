package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Coupon 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    /**
     * 쿠폰 코드로 쿠폰을 조회합니다.
     *
     * @param code 쿠폰 코드
     * @return 조회된 쿠폰을 담은 Optional
     */
    Optional<Coupon> findByCode(String code);
}

