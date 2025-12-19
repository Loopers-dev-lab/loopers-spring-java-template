package com.loopers.infrastructure.coupon.event;

import com.loopers.infrastructure.event.BaseInboxEventRepository;
import com.loopers.domain.coupon.event.CouponInboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponInboxEventRepository extends BaseInboxEventRepository<CouponInboxEvent> {
}

