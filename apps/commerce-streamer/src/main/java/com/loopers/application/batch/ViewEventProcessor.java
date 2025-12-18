package com.loopers.application.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewEventProcessor {

  private final EventHandledService eventHandledService;
  private final ProductMetricsService productMetricsService;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, String> redisTemplate;

  @Scheduled(fixedDelay = 15000) // 15초마다 실행 (배치 처리)
  public void processPendingViewEvents() {
    // 조회 이벤트만 조회
    List<EventHandled> viewEvents = eventHandledService.findPendingEventsByType("ProductViewed");

    if (viewEvents.isEmpty()) {
      return;
    }

    // 중복 이벤트 제거 (eventId 기준 + businessKey 기준)
    Map<String, EventHandled> latestEvents = selectLatestEvents(viewEvents);

    // 오래된 이벤트들을 COMPLETED로 처리
    markObsoleteEventsAsCompleted(viewEvents, latestEvents);

    log.info("Processing {} latest view events out of {} total",
             latestEvents.size(), viewEvents.size());

    // 최신 이벤트들만 처리
    for (EventHandled event : latestEvents.values()) {
      try {
        // 처리 중 상태로 변경 (동시성 제어)
        eventHandledService.markAsProcessing(event.getId());

        // 실제 비즈니스 로직 처리
        processEvent(event);

        // 처리 완료
        eventHandledService.markAsCompleted(event.getId());

      } catch (Exception e) {
        log.error("Failed to process view event: businessKey={}, error={}",
            event.getBusinessKey(), e.getMessage(), e);

        if (event.canRetry()) {
          // 재시도 가능하면 FAILED로 마킹
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

    if ("ProductViewed".equals(event.getEventType())) {
      processProductViewed(eventData, event);
    } else {
      log.warn("Unknown view event type: {}", event.getEventType());
    }
  }

  private void processProductViewed(JsonNode eventData, EventHandled event) {
    Long productId = eventData.get("productId").asLong();
    Long userId = eventData.get("userId").asLong();

    log.info("Processing ProductViewed event: productId={}, userId={}, eventId={}",
        productId, userId, event.getEventId());

    // 조회수 증가
    productMetricsService.incrementViewCount(productId);
    
    // 상품 캐시 삭제
    evictProductCache(productId);

    log.info("ProductViewed metrics updated: productId={}, eventId={}",
        productId, event.getEventId());
  }

  /**
   * 중복 이벤트 제거 로직
   * 1. eventId가 같으면 최신 createdAt 기준으로 선택
   * 2. businessKey가 같으면 최신 eventId 기준으로 선택
   */
  private Map<String, EventHandled> selectLatestEvents(List<EventHandled> events) {
    Map<String, EventHandled> result = new HashMap<>();

    // 1단계: eventId 기준으로 그룹핑하고 최신 데이터 선택
    Map<String, EventHandled> latestByEventId = events.stream()
        .collect(Collectors.groupingBy(
            EventHandled::getEventId,
            Collectors.maxBy((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt()))
        ))
        .values()
        .stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(
            EventHandled::getEventId,
            event -> event
        ));

    // 2단계: businessKey 기준으로 그룹핑하고 최신 eventId 선택
    Map<String, EventHandled> latestByBusinessKey = latestByEventId.values().stream()
        .collect(Collectors.groupingBy(
            EventHandled::getBusinessKey,
            Collectors.maxBy((e1, e2) -> e1.getEventId().compareTo(e2.getEventId()))
        ))
        .values()
        .stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(
            event -> event.getEventId() + ":" + event.getBusinessKey(),
            event -> event
        ));

    return latestByBusinessKey;
  }

  /**
   * 선택되지 않은 오래된 이벤트들을 COMPLETED로 처리
   */
  private void markObsoleteEventsAsCompleted(List<EventHandled> allEvents, Map<String, EventHandled> latestEvents) {
    Set<Long> latestEventIds = latestEvents.values().stream()
        .map(EventHandled::getId)
        .collect(Collectors.toSet());

    for (EventHandled event : allEvents) {
      if (!latestEventIds.contains(event.getId())) {
        try {
          eventHandledService.markAsCompleted(event.getId());
          log.info("Marked obsolete view event as completed: eventId={}, businessKey={}, id={}", 
                   event.getEventId(), event.getBusinessKey(), event.getId());
        } catch (Exception e) {
          log.warn("Failed to mark obsolete view event as completed: eventId={}", 
                   event.getEventId(), e);
        }
      }
    }
  }

  /**
   * 상품 상세 캐시 삭제
   */
  private void evictProductCache(Long productId) {
    try {
      String cacheKey = "product:detail:" + productId;
      redisTemplate.delete(cacheKey);
      log.info("Product cache evicted for view event: productId={}", productId);
    } catch (Exception e) {
      log.warn("Failed to evict product cache for view event: productId={}, error={}", 
               productId, e.getMessage());
    }
  }
}