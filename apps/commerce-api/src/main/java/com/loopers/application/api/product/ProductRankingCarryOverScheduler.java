package com.loopers.application.api.product;

import com.loopers.core.service.product.ProductRankingCarryOverService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRankingCarryOverScheduler {

    private final ProductRankingCarryOverService service;

    @Scheduled(cron = "0 50 23 * * *")
    public void carryOver() {
        service.carryOver();
    }
}
