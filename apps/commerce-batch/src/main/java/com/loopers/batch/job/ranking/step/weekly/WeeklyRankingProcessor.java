package com.loopers.batch.job.ranking.step.weekly;

import com.loopers.domain.ProductMetrics;
import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class WeeklyRankingProcessor implements ItemProcessor<ProductMetrics, WeeklyRankingWork> {

  private int rankCounter = 0;

  @Override
  public WeeklyRankingWork process(ProductMetrics item) {
    rankCounter++;
    if (rankCounter > 100) {
      return null;
    }

    Double score = (item.getViewCount() * 0.1) + (item.getLikeCount() * 0.2) + (item.getSalesCount() * 0.6);

    return new WeeklyRankingWork(
        item.getProductId(),
        score,
        rankCounter
    );
  }
}
