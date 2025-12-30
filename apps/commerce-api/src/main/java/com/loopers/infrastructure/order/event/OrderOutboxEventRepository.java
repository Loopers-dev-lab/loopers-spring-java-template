package com.loopers.infrastructure.order.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.order.event.OrderOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxEventRepository extends BaseOutboxEventRepository<OrderOutboxEvent> {
}

