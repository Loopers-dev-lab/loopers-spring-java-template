package com.loopers.domain.payment.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.event.outbox.AbstractOutboxEventProcessor;
import com.loopers.infrastructure.payment.event.PaymentOutboxEventRepository;
import com.loopers.lock.DistributedLockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxProcessor extends AbstractOutboxEventProcessor<PaymentOutboxEvent> {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

    public PaymentOutboxProcessor(KafkaTemplate<String, String> stringKafkaTemplate,
                                   DistributedLockService distributedLockService,
                                   PaymentOutboxEventRepository paymentOutboxEventRepository) {
        super(stringKafkaTemplate, distributedLockService);
        this.paymentOutboxEventRepository = paymentOutboxEventRepository;
    }

    @Override
    protected BaseOutboxEventRepository<PaymentOutboxEvent> getRepository() {
        return paymentOutboxEventRepository;
    }

    @Override
    protected String getLockKey() {
        return "outbox:payment:lock";
    }

    @Override
    protected String getDomainName() {
        return "PAYMENT";
    }

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void process() {
        processPendingEvents();
    }
}

