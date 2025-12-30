package com.loopers.infrastructure.stock.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.stock.event.StockOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface StockOutboxEventRepository extends BaseOutboxEventRepository<StockOutboxEvent> {
}

