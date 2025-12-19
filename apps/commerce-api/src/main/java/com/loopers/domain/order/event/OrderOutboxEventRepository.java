package com.loopers.domain.order.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxEventRepository extends BaseOutboxEventRepository<OrderOutboxEvent> {
}

