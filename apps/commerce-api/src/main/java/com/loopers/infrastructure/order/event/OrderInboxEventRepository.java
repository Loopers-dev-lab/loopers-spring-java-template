package com.loopers.infrastructure.order.event;

import com.loopers.infrastructure.event.BaseInboxEventRepository;
import com.loopers.domain.order.event.OrderInboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInboxEventRepository extends BaseInboxEventRepository<OrderInboxEvent> {
}

