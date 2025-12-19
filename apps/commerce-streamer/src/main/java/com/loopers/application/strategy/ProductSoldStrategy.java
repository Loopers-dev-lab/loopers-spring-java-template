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
public class ProductSoldStrategy implements CatalogEventStrategy {

  private final ProductMetricsRepository productMetricsRepository;

  @Override
  public boolean supports(String eventType) {
    return EventType.PRODUCT_SOLD.matches(eventType);
  }

  @Override
  public void handle(Long productId, Long occurredAt, JsonNode payload) {
    int quantity = extractQuantity(payload, productId);
    productMetricsRepository.upsertSalesCount(productId, quantity, occurredAt);
    log.debug("상품 {} 판매 수량 {} 증가", productId, quantity);
  }

  private int extractQuantity(JsonNode payload, Long productId) {
    if (!payload.has("quantity")) {
      throw new IllegalArgumentException("PRODUCT_SOLD 이벤트에 quantity 필드 누락: productId=" + productId);
    }

    JsonNode quantityNode = payload.get("quantity");
    if (!quantityNode.isInt()) {
      throw new IllegalArgumentException("quantity는 정수여야 합니다: productId=" + productId);
    }

    int quantity = quantityNode.asInt();
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity는 양수여야 합니다: productId=" + productId + ", quantity=" + quantity);
    }

    return quantity;
  }
}
