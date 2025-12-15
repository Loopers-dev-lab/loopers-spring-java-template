package com.loopers.application.api.payment;

import com.loopers.core.service.payment.PaymentCompletedEventPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCompletedEventScheduler {

    private final PaymentCompletedEventPublishService service;

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        service.publish();
    }
}
