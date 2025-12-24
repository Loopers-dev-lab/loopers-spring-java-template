package com.loopers.core.service.product;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductViewRankingScoreCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IncreaseProductViewRankingScoreService {

    private final ProductRankingCacheRepository productRankingCacheRepository;

    @Value("${product.ranking.score.weight.view}")
    private double weight;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_PRODUCT_VIEW_RANKING_SCORE",
            eventIdField = "eventId",
            aggregateIdField = "id"
    )
    public void increase(IncreaseProductViewRankingScoreCommand command) {
        ProductId productId = new ProductId(command.productId());
        productRankingCacheRepository.increaseDaily(productId, LocalDate.now(), weight);
    }
}
