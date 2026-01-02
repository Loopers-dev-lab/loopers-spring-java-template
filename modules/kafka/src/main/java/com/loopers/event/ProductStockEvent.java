package com.loopers.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductStockEvent(
    String eventId,
    Long productId,
    String productName,
    int sellQuantity,
    int currentStock,
    long price,
    LocalDateTime createdAt
) {
  public static ProductStockEvent of(Long productId, String productName, int sellQuantity, int currentStock, long price) {
    return new ProductStockEvent(
        UUID.randomUUID().toString(),
        productId,
        productName,
        sellQuantity,
        currentStock,
        price,
        LocalDateTime.now()
    );
  }
}
