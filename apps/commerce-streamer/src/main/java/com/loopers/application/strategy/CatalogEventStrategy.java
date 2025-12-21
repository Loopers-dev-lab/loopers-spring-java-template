package com.loopers.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;

public interface CatalogEventStrategy {

  boolean supports(String eventType);

  void handle(Long productId, Long occurredAt, JsonNode payload);
}