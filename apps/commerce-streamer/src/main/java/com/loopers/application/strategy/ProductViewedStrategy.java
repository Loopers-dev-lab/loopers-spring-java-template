package com.loopers.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.event.EventType;
import com.loopers.domain.metrics.MetricDateConverter;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.RankingRedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedStrategy implements CatalogEventStrategy {

  private final ProductMetricsRepository productMetricsRepository;
  private final RankingRedisProperties rankingProperties;

  @Override
  public boolean supports(String eventType) {
    return EventType.PRODUCT_VIEWED.matches(eventType);
  }

  @Override
  public void handle(Long productId, Long occurredAt, JsonNode payload) {
    Integer metricDate = MetricDateConverter.toMetricDate(occurredAt, rankingProperties.getTimezone());
    productMetricsRepository.upsertViewCount(productId, metricDate, 1, occurredAt);
    log.debug("상품 {} 조회 수 증가 (날짜: {})", productId, metricDate);
  }
}
