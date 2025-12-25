package com.loopers.application.api.product;

import com.loopers.core.service.product.ProductOutOfStockEventPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductOutOfStockEventScheduler {

    private final ProductOutOfStockEventPublishService service;

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        service.publish();
    }
}
