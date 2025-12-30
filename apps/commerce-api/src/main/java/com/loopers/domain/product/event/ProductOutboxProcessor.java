package com.loopers.domain.product.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.infrastructure.product.event.ProductOutboxEventRepository;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProductOutboxProcessor extends AbstractOutboxEventProcessor<ProductOutboxEvent> {

    private final ProductOutboxEventRepository productOutboxEventRepository;

    public ProductOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                   DistributedLockService distributedLockService,
                                   ProductOutboxEventRepository productOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.productOutboxEventRepository = productOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<ProductOutboxEvent> getRepository() {
        return productOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:product:lock";
    }

    @Override
    protected String getDomainName() {
        return "PRODUCT";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

