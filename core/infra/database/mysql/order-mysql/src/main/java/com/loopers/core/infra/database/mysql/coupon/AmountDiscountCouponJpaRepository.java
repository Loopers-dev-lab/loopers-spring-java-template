package com.loopers.core.infra.database.mysql.coupon;

import com.loopers.core.infra.database.mysql.coupon.entity.AmountDiscountCouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AmountDiscountCouponJpaRepository extends JpaRepository<AmountDiscountCouponEntity, Long> {

    Optional<AmountDiscountCouponEntity> findByCouponId(Long couponId);
}
