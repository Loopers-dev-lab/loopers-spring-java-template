package com.loopers.domain.stock.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.infrastructure.stock.event.StockOutboxEventRepository;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockOutboxProcessor extends AbstractOutboxEventProcessor<StockOutboxEvent> {

    private final StockOutboxEventRepository stockOutboxEventRepository;

    public StockOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                DistributedLockService distributedLockService,
                                StockOutboxEventRepository stockOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.stockOutboxEventRepository = stockOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<StockOutboxEvent> getRepository() {
        return stockOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:stock:lock";
    }

    @Override
    protected String getDomainName() {
        return "STOCK";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

