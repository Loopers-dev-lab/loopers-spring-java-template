package com.loopers.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.event.EventType;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedStrategy implements CatalogEventStrategy {

  private final ProductMetricsRepository productMetricsRepository;

  @Override
  public boolean supports(String eventType) {
    return EventType.PRODUCT_VIEWED.matches(eventType);
  }

  @Override
  public void handle(Long productId, Long occurredAt, JsonNode payload) {
    productMetricsRepository.upsertViewCount(productId, 1, occurredAt);
    log.debug("상품 {} 조회 수 증가", productId);
  }
}