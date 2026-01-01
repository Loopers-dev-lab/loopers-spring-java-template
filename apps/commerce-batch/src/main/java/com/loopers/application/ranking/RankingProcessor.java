package com.loopers.application.ranking;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.ranking.ProductRankingAggregate;
import com.loopers.domain.ranking.RankingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class RankingProcessor implements ItemProcessor<ProductMetrics, ProductRankingAggregate> {

    private final RankingCalculator rankingCalculator;

    @Override
    public ProductRankingAggregate process(ProductMetrics item) throws Exception {
        ProductRankingAggregate aggregate = new ProductRankingAggregate(item.getProductId());
        aggregate.addMetrics(item.getLikeCount(), item.getViewCount(), item.getSalesCount());
        aggregate.calculateScore(rankingCalculator);
        return aggregate;
    }
}

