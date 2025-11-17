package com.loopers.core.infra.database.mysql.coupon;

import com.loopers.core.infra.database.mysql.coupon.entity.RateDiscountCouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateDiscountCouponJpaRepository extends JpaRepository<RateDiscountCouponEntity, Long> {

    Optional<RateDiscountCouponEntity> findByCouponId(Long couponId);
}
