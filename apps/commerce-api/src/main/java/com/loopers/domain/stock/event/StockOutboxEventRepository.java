package com.loopers.domain.stock.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockOutboxEventRepository extends BaseOutboxEventRepository<StockOutboxEvent> {
}

