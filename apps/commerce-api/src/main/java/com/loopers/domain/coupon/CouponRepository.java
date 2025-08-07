package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    CouponModel save(CouponModel coupon);
    Optional<CouponModel> findById(Long id);
    List<CouponModel> findByUserId(Long userId);
    List<CouponModel> findUsableCouponsByUserId(Long userId);
    List<CouponModel> findByOrderId(Long orderId);
    boolean existsById(Long id);
    Optional<CouponModel> findByIdForUpdate(Long id);
    void deleteAll();
}
