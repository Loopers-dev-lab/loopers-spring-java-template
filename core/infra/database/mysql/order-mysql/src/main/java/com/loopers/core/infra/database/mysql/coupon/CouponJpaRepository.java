package com.loopers.core.infra.database.mysql.coupon;

import com.loopers.core.infra.database.mysql.coupon.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {
}
