package com.loopers.domain.coupon.event;

import com.loopers.domain.event.BaseInboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponInboxEventRepository extends BaseInboxEventRepository<CouponInboxEvent> {
}

