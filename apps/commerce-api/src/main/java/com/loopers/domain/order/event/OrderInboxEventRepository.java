package com.loopers.domain.order.event;

import com.loopers.domain.event.BaseInboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInboxEventRepository extends BaseInboxEventRepository<OrderInboxEvent> {
}

