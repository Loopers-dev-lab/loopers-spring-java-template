package com.loopers.application.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessor {

  private final EventHandledService eventHandledService;
  private final ProductMetricsService productMetricsService;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, String> redisTemplate;

  @Scheduled(fixedDelay = 5000) // 5초마다 실행
  public void processPendingEvents() {
    // 주문 생성 이벤트만 먼저 조회
    List<EventHandled> orderEvents = eventHandledService.findPendingEventsByType("OrderCreated");

    if (orderEvents.isEmpty()) {
      return;
    }

    // eventId별로 그룹핑하고 최신 이벤트만 선택 (배치 처리)
    Map<String, EventHandled> latestEventsByEventId = selectLatestEventsByOrderId(orderEvents);

    // 오래된 이벤트들은 COMPLETED로 처리
    markObsoleteEventsAsSkipped(orderEvents, latestEventsByEventId);

    log.info("Processing {} latest order events out of {} total",
        latestEventsByEventId.size(), orderEvents.size());

    // 최신 이벤트들만 처리
    for (EventHandled event : latestEventsByEventId.values()) {
      try {
        // 처리 중 상태로 변경 (동시성 제어)
        eventHandledService.markAsProcessing(event.getId());

        // 실제 비즈니스 로직 처리
        processEvent(event);

        // 처리 완료
        eventHandledService.markAsCompleted(event.getId());

      } catch (Exception e) {
        log.error("Failed to process order event: businessKey={}, error={}",
            event.getBusinessKey(), e.getMessage(), e);

        if (event.canRetry()) {
          // 재시도 가능하면 PENDING으로 되돌림
          eventHandledService.markAsFailed(event.getId(), e.getMessage());
        } else {
          // 최대 재시도 횟수 초과시 FAILED로 마킹
          eventHandledService.markAsFailed(event.getId(),
              "Max retry exceeded: " + e.getMessage());
        }
      }
    }
  }

  private void processEvent(EventHandled event) throws Exception {
    JsonNode eventData = objectMapper.readTree(event.getPayload());

    // 주문 생성 이벤트만 처리
    if ("OrderCreated".equals(event.getEventType())) {
      processOrderCreated(eventData, event);
    }
  }

  private void processOrderCreated(JsonNode eventData, EventHandled event) {
    Long orderId = eventData.get("orderId").asLong();
    Long userId = eventData.get("userId").asLong();

    log.info("Processing OrderCreated event: orderId={}, userId={}, eventId={}",
        orderId, userId, event.getBusinessKey());

    if (eventData.has("orderItems") && eventData.get("orderItems").isArray()) {
      eventData.get("orderItems").forEach(item -> {
        JsonNode itemNode = (JsonNode) item;
        if (itemNode.isObject()) {
          Long productId = itemNode.get("productId").asLong();
          Long quantity = itemNode.get("quantity").asLong();
          productMetricsService.incrementSalesCount(productId, quantity);
          
          // 상품 캐시 삭제
          evictProductCache(productId);
        }
      });
    }

    log.info("OrderCreated metrics updated: orderId={}, eventId={}",
        orderId, event.getBusinessKey());
  }

  /**
   * 상품 상세 캐시 삭제
   */
  private void evictProductCache(Long productId) {
    try {
      String cacheKey = "product:detail:" + productId;
      redisTemplate.delete(cacheKey);
      log.info("Product cache evicted: productId={}", productId);
    } catch (Exception e) {
      log.warn("Failed to evict product cache: productId={}, error={}", productId, e.getMessage());
    }
  }



  /**
   * eventId별로 그룹핑하고 최신 이벤트만 선택 (같은 eventId면 최신 데이터만 처리)
   */
  private Map<String, EventHandled> selectLatestEventsByOrderId(List<EventHandled> orderEvents) {
    return orderEvents.stream()
        .collect(Collectors.groupingBy(
            EventHandled::getEventId, // eventId로 그룹핑
            Collectors.maxBy((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt())) // 최신 createdAt
        ))
        .values()
        .stream()
        .filter(opt -> opt.isPresent())
        .map(opt -> opt.get())
        .collect(Collectors.toMap(
            EventHandled::getEventId,
            event -> event
        ));
  }

  /**
   * 중복된 eventId의 오래된 이벤트들을 COMPLETED로 처리
   */
  private void markObsoleteEventsAsSkipped(List<EventHandled> allEvents, Map<String, EventHandled> latestEvents) {
    for (EventHandled event : allEvents) {
      EventHandled latestEvent = latestEvents.get(event.getEventId());
      
      // 최신 이벤트가 아니면 COMPLETED로 처리
      if (latestEvent == null || !latestEvent.getId().equals(event.getId())) {
        try {
          eventHandledService.markAsCompleted(event.getId());
          log.info("Marked obsolete event as completed: eventId={}, id={}", 
                   event.getEventId(), event.getId());
        } catch (Exception e) {
          log.warn("Failed to mark obsolete event as completed: eventId={}", event.getEventId(), e);
        }
      }
    }
  }

}
