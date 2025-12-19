package com.loopers.infrastructure.coupon.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.coupon.event.CouponOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponOutboxEventRepository extends BaseOutboxEventRepository<CouponOutboxEvent> {
}

