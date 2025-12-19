package com.loopers.domain.coupon.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.infrastructure.coupon.event.CouponOutboxEventRepository;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CouponOutboxProcessor extends AbstractOutboxEventProcessor<CouponOutboxEvent> {

    private final CouponOutboxEventRepository couponOutboxEventRepository;

    public CouponOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                 DistributedLockService distributedLockService,
                                 CouponOutboxEventRepository couponOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.couponOutboxEventRepository = couponOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<CouponOutboxEvent> getRepository() {
        return couponOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:coupon:lock";
    }

    @Override
    protected String getDomainName() {
        return "COUPON";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

