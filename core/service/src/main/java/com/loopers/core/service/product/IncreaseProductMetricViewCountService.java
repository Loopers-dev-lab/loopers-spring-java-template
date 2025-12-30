package com.loopers.core.service.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductMetricViewCountCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncreaseProductMetricViewCountService {

    private final DailyProductMetricRepository dailyProductMetricRepository;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_PRODUCT_VIEW_COUNT",
            eventIdField = "eventId",
            aggregateIdField = "productId"
    )
    @Transactional
    public DailyProductMetric increase(IncreaseProductMetricViewCountCommand command) {
        DailyProductMetric metric = dailyProductMetricRepository.findByWithLock(new ProductId(command.productId()), new CreatedAt(LocalDateTime.now()))
                .orElse(DailyProductMetric.init(new ProductId(command.productId())));

        return dailyProductMetricRepository.save(metric.increaseViewCount());
    }
}
