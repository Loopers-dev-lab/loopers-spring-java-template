package com.loopers.domain.like.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LikeOutboxProcessor extends AbstractOutboxEventProcessor<LikeOutboxEvent> {

    private final LikeOutboxEventRepository likeOutboxEventRepository;

    public LikeOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                DistributedLockService distributedLockService,
                                LikeOutboxEventRepository likeOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.likeOutboxEventRepository = likeOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<LikeOutboxEvent> getRepository() {
        return likeOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:like:lock";
    }

    @Override
    protected String getDomainName() {
        return "LIKE";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

