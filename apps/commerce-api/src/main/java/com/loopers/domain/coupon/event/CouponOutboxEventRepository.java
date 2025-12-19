package com.loopers.domain.coupon.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponOutboxEventRepository extends BaseOutboxEventRepository<CouponOutboxEvent> {
}

