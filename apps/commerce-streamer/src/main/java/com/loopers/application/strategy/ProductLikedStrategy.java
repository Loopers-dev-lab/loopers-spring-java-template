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
public class ProductLikedStrategy implements CatalogEventStrategy {

  private final ProductMetricsRepository productMetricsRepository;

  @Override
  public boolean supports(String eventType) {
    return EventType.PRODUCT_LIKED.matches(eventType);
  }

  @Override
  public void handle(Long productId, Long occurredAt, JsonNode payload) {
    productMetricsRepository.upsertLikeCount(productId, 1, occurredAt);
    log.debug("상품 {} 좋아요 수 증가", productId);
  }
}