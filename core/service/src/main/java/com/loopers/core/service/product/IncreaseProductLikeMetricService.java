package com.loopers.core.service.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductLikeMetricCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncreaseProductLikeMetricService {

    private final ProductMetricRepository productMetricRepository;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_PRODUCT_METRIC_LIKE_COUNT",
            eventIdField = "eventId",
            aggregateIdField = "productId"
    )
    @Transactional
    public void increase(IncreaseProductLikeMetricCommand command) {
        DailyProductMetric metric = productMetricRepository.findByWithLock(new ProductId(command.productId()), new CreatedAt(LocalDateTime.now()))
                .orElse(DailyProductMetric.init(new ProductId(command.productId())));

        productMetricRepository.save(metric.increaseLikeCount());
    }
}
