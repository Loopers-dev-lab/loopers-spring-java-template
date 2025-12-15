package com.loopers.core.service.product;

import com.loopers.core.domain.product.ProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductMetricViewCountCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncreaseProductMetricViewCountService {

    private final ProductMetricRepository productMetricRepository;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_PRODUCT_VIEW_COUNT",
            eventIdField = "eventId",
            aggregateIdField = "productId"
    )
    @Transactional
    public ProductMetric increase(IncreaseProductMetricViewCountCommand command) {
        ProductMetric metric = productMetricRepository.findByWithLock(new ProductId(command.productId()))
                .orElse(ProductMetric.init(new ProductId(command.productId())));

        return productMetricRepository.save(metric.increaseViewCount());
    }
}
