package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.embeded.CouponUserId;
import com.loopers.domain.coupon.embeded.CouponOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CouponJpaRepository extends JpaRepository<CouponModel, Long> {
    
    List<CouponModel> findByUserId(CouponUserId userId);
    
    @Query("SELECT c FROM CouponModel c WHERE c.userId = :userId AND c.used.used = false AND c.expiredAt.expiredAt > CURRENT_TIMESTAMP")
    List<CouponModel> findUsableCouponsByUserId(@Param("userId") CouponUserId userId);
    
    List<CouponModel> findByOrderId(CouponOrderId orderId);
}