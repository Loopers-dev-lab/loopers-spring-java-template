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
     * 쿠폰을 저장합니다.
     *
     * @param coupon 저장할 쿠폰
     * @return 저장된 쿠폰
     */
    Coupon save(Coupon coupon);

    /**
     * 쿠폰 코드로 쿠폰을 조회합니다.
     *
     * @param code 쿠폰 코드
     * @return 조회된 쿠폰을 담은 Optional
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 쿠폰 ID로 쿠폰을 조회합니다.
     *
     * @param couponId 쿠폰 ID
     * @return 조회된 쿠폰을 담은 Optional
     */
    Optional<Coupon> findById(Long couponId);
}

