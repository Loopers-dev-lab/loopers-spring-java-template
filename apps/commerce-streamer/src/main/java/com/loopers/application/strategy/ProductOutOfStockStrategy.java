package com.loopers.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutOfStockStrategy implements CatalogEventStrategy {

  private static final String PRODUCT_CACHE_PREFIX = "product:v1:";

  private final StringRedisTemplate redisTemplate;

  @Override
  public boolean supports(String eventType) {
    return EventType.PRODUCT_OUT_OF_STOCK.matches(eventType);
  }

  @Override
  public void handle(Long productId, Long occurredAt, JsonNode payload) {
    String cacheKey = PRODUCT_CACHE_PREFIX + productId;
    redisTemplate.delete(cacheKey);
    log.debug("상품 {} 캐시 삭제 (재고 소진)", productId);
  }
}
