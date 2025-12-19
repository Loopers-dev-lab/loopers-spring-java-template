package com.loopers.domain.stock.event;

import com.loopers.domain.event.BaseInboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockInboxEventRepository extends BaseInboxEventRepository<StockInboxEvent> {
}

