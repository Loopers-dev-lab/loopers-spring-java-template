package com.loopers.core.service.productlike;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.productlike.command.IncreaseProductLikeRankingScoreCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncreaseProductLikeRankingScoreService {

    private final ProductRankingCacheRepository productRankingCacheRepository;

    @Value("${product.ranking.score.weight}")
    private double weight;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_PRODUCT_LIKE_RANKING_SCORE",
            eventIdField = "eventId",
            aggregateIdField = "productId"
    )
    public void increase(IncreaseProductLikeRankingScoreCommand command) {
        ProductId productId = new ProductId(command.productId());
        productRankingCacheRepository.increaseDaily(productId, LocalDateTime.now(), weight);
    }
}
