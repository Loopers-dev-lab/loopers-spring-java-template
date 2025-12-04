package com.loopers.application.api.productlike;

import com.loopers.core.service.productlike.ProductLikeSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductLikeSyncScheduler {

    private final ProductLikeSyncService productLikeSyncService;

    @Scheduled(fixedDelay = 1000)
    public void sync() {
        productLikeSyncService.sync();
    }
}
