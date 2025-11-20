package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);

    Optional<UserCoupon> findById(Long id);

    /**
     * 동시성 제어를 위한 비관적 락을 사용하는 조회 메서드
     *
     * @param id UserCoupon ID
     * @return UserCoupon
     */
    Optional<UserCoupon> findByIdWithLock(Long id);

    List<UserCoupon> findByUserId(String userId);

    List<UserCoupon> findByUserIdAndIsAvailable(String userId);
}
