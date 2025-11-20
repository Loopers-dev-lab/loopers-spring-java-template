package com.loopers.domain.coupon;

import java.util.Optional;

/**
 * Coupon 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 쿠폰 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface CouponRepository {
    /**
 * Persist the given coupon and return the stored instance.
 *
 * @param coupon the Coupon to persist
 * @return the persisted Coupon instance
 */
    Coupon save(Coupon coupon);

    /**
 * Finds a coupon by its code.
 *
 * @param code the coupon's unique code
 * @return an Optional containing the matching Coupon if present, empty otherwise
 */
    Optional<Coupon> findByCode(String code);

    /**
 * Retrieve a coupon by its identifier.
 *
 * @param couponId the coupon's identifier
 * @return an Optional containing the Coupon if found, or empty if not
 */
    Optional<Coupon> findById(Long couponId);
}
