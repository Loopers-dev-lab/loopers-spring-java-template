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
 * Finds a coupon by its code.
 *
 * @param code the coupon code to look up
 * @return an Optional containing the Coupon if found, otherwise an empty Optional
 */
    Optional<Coupon> findByCode(String code);
}
