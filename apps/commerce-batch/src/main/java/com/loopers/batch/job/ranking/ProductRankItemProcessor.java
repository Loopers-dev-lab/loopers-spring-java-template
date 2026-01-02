package com.loopers.batch.job.ranking;

import com.loopers.batch.domain.metrics.ProductMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRankItemProcessor implements ItemProcessor<ProductMetrics, ProductRankAggregate> {

    private final ProductRankScorePolicy scorePolicy;

    @Override
    public ProductRankAggregate process(ProductMetrics item) {
        if (item == null || item.getProductId() == null) {
            return null;
        }
        double score = scorePolicy.calculate(item.getLikeCount(), item.getSalesCount());
        return new ProductRankAggregate(
                item.getProductId(),
                item.getLikeCount(),
                item.getSalesCount(),
                score
        );
    }
}
