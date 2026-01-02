package com.loopers.domain.metrics;

import com.loopers.core.cache.RedisCacheHandler;
import com.loopers.domain.ProductMetrics;
import com.loopers.domain.event.EventHandled;
import com.loopers.event.LikeCountEvent;
import com.loopers.event.ProductStockEvent;
import com.loopers.event.ProductViewEvent;
import com.loopers.infrastructure.EventHandledRepository;
import com.loopers.infrastructure.ProductMetricsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductMetricsService {

  private final ProductMetricsRepository metricsRepository;
  private final EventHandledRepository eventHandledRepository;
  private final RedisCacheHandler redisCacheHandler;

  @Transactional
  public void processLikeCountEvent(LikeCountEvent event) {
    if (isAlreadyHandled(event.eventId())) return;

    ProductMetrics metrics = getOrCreateMetrics(event.productId());

    metrics.updateLikeCount(event.currentLikeCount(), LocalDateTime.now());

    completeProcess(event.eventId(), metrics);
  }

  @Transactional
  public void processProductViewEvent(ProductViewEvent event) {
    if (isAlreadyHandled(event.eventId())) return;

    ProductMetrics metrics = getOrCreateMetrics(event.productId());

    metrics.incrementViewCount();

    completeProcess(event.eventId(), metrics);
  }

  @Transactional
  public void processSalesCountEvent(ProductStockEvent event) {
    if (isAlreadyHandled(event.eventId())) return;

    ProductMetrics metrics = getOrCreateMetrics(event.productId());

    metrics.addSalesCount(event.sellQuantity());

    metrics.updateProductSnapshot(
        event.productName(),
        event.price(),
        event.currentStock()
    );

    if (event.currentStock() <= 0) {
      redisCacheHandler.delete("product:detail:" + event.productId());
      redisCacheHandler.deleteByPattern("product:list");

    }

    completeProcess(event.eventId(), metrics);
  }

  private boolean isAlreadyHandled(String eventId) {
    return eventHandledRepository.existsById(eventId);
  }

  private ProductMetrics getOrCreateMetrics(Long productId) {
    return metricsRepository.findById(productId)
        .orElseGet(() -> new ProductMetrics(productId));
  }

  private void completeProcess(String eventId, ProductMetrics metrics) {
    metricsRepository.save(metrics);
    eventHandledRepository.save(new EventHandled(eventId));
  }
}
