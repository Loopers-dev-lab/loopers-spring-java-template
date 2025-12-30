package com.loopers.infrastructure.stock.event;

import com.loopers.infrastructure.event.BaseInboxEventRepository;
import com.loopers.domain.stock.event.StockInboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface StockInboxEventRepository extends BaseInboxEventRepository<StockInboxEvent> {
}

