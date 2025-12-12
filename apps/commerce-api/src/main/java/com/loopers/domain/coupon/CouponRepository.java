package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 레포지토리 인터페이스
 * - Infrastructure 레이어에서 구현
 */
public interface CouponRepository {

    /**
     * 쿠폰 저장
     */
    Coupon save(Coupon coupon);

    /**
     * ID로 쿠폰 조회
     */
    Optional<Coupon> findById(Long id);

    /**
     * ID로 쿠폰 조회 (비관적 락)
     */
    Optional<Coupon> findByIdWithLock(Long id);

    /**
     * 사용자 ID로 쿠폰 목록 조회
     */
    List<Coupon> findByUserId(String userId);

    /**
     * 사용자 ID와 사용 가능 여부로 쿠폰 목록 조회
     */
    List<Coupon> findAvailableCouponsByUserId(String userId);

    /**
     * 쿠폰 삭제
     */
    void delete(Coupon coupon);

    /**
     * 쿠폰 존재 여부 확인
     */
    boolean existsById(Long id);
}
