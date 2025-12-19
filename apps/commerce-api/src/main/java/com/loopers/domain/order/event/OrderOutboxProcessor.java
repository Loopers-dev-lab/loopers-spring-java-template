package com.loopers.domain.order.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxProcessor extends AbstractOutboxEventProcessor<OrderOutboxEvent> {

    private final OrderOutboxEventRepository orderOutboxEventRepository;

    public OrderOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                DistributedLockService distributedLockService,
                                OrderOutboxEventRepository orderOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.orderOutboxEventRepository = orderOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<OrderOutboxEvent> getRepository() {
        return orderOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:order:lock";
    }

    @Override
    protected String getDomainName() {
        return "ORDER";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

