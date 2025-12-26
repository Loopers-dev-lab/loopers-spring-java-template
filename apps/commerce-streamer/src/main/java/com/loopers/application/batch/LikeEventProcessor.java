package com.loopers.application.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import com.loopers.domain.ranking.ProductRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeEventProcessor {

  private final EventHandledService eventHandledService;
  private final ProductRankingService productRankingService;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, String> redisTemplate;

  @Scheduled(fixedDelay = 10000) // 10초마다 실행 (배치 처리)
  public void processPendingLikeEvents() {
    // 좋아요 관련 이벤트만 조회
    List<EventHandled> likeEvents = new ArrayList<>();
    likeEvents.addAll(eventHandledService.findPendingEventsByType("ProductLiked"));
    likeEvents.addAll(eventHandledService.findPendingEventsByType("ProductUnliked"));

    if (likeEvents.isEmpty()) {
      return;
    }

    // 중복 이벤트 제거 (eventId 기준 + businessKey 기준)
    Map<String, EventHandled> latestEvents = selectLatestEvents(likeEvents);

    // 오래된 이벤트들을 COMPLETED로 처리
    markObsoleteEventsAsCompleted(likeEvents, latestEvents);

    log.info("Processing {} latest like events out of {} total",
        latestEvents.size(), likeEvents.size());

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
        log.error("Failed to process like event: businessKey={}, error={}",
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

    switch (event.getEventType()) {
      case "ProductLiked" -> processProductLiked(eventData, event);
      case "ProductUnliked" -> processProductUnliked(eventData, event);
      default -> log.warn("Unknown like event type: {}", event.getEventType());
    }
  }

  private void processProductLiked(JsonNode eventData, EventHandled event) {
    Long productId = eventData.get("productId").asLong();
    Long userId = eventData.get("userId").asLong();
    ZonedDateTime eventTime = event.getEventTime();
    LocalDateTime bucketTime = getBucketTime(eventTime);
    String bucketTimeKey = getBucketTimeKey(bucketTime);

    log.info("Processing ProductLiked event: productId={}, userId={}, eventId={}, eventTime={}, bucketTime={}",
        productId, userId, event.getEventId(), eventTime, bucketTimeKey);

    // eventTime 기준 10분 간격 좋아요수 누적 (Redis만 즉시 반영)
    incrementLikeCountByBucketTime(productId, bucketTimeKey);

    // 랭킹 점수 추가
    productRankingService.addLikeScore(productId);

    // 상품 캐시 삭제
    evictProductCache(productId);

    log.info("ProductLiked metrics and ranking updated: productId={}, eventId={}",
        productId, event.getEventId());
  }

  private void processProductUnliked(JsonNode eventData, EventHandled event) {
    Long productId = eventData.get("productId").asLong();
    Long userId = eventData.get("userId").asLong();
    ZonedDateTime eventTime = event.getEventTime();
    LocalDateTime bucketTime = getBucketTime(eventTime);
    String bucketTimeKey = getBucketTimeKey(bucketTime);

    log.info("Processing ProductUnliked event: productId={}, userId={}, eventId={}, eventTime={}, bucketTime={}, bucketTimeKey={}",
        productId, userId, event.getEventId(), eventTime, bucketTime, bucketTimeKey);

    // eventTime 기준 10분 간격 좋아요 취소 누적 (Redis만 즉시 반영)
    decrementLikeCountByBucketTime(productId, bucketTimeKey);

    // 랭킹 점수 감소
    productRankingService.subtractLikeScore(productId);

    // 상품 캐시 삭제
    evictProductCache(productId);

    log.info("ProductUnliked metrics and ranking updated: productId={}, eventId={}",
        productId, event.getEventId());
  }

  /**
   * 중복 이벤트 제거 로직 (eventTime 기준 최종 상태 결정)
   * 1. eventId가 같으면 최신 createdAt 기준으로 선택
   * 2. businessKey가 같으면 eventTime 기준으로 최종 상태 결정
   */
  private Map<String, EventHandled> selectLatestEvents(List<EventHandled> events) {
    Map<String, EventHandled> result = new HashMap<>();

    // 1단계: eventId 기준으로 중복 제거 (네트워크 레벨 중복)
    Map<String, EventHandled> latestByEventId = events.stream()
        .collect(Collectors.groupingBy(
            EventHandled::getEventId,
            Collectors.maxBy((e1, e2) -> e1.getEventTime().compareTo(e2.getEventTime()))
        ))
        .values()
        .stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(
            EventHandled::getEventId,
            event -> event
        ));

    // 2단계: businessKey 기준으로 eventTime 최신 이벤트 선택 (최종 상태 결정)
    Map<String, List<EventHandled>> eventsByBusinessKey = latestByEventId.values().stream()
        .collect(Collectors.groupingBy(EventHandled::getBusinessKey));

    for (Map.Entry<String, List<EventHandled>> entry : eventsByBusinessKey.entrySet()) {
      String businessKey = entry.getKey();
      List<EventHandled> businessKeyEvents = entry.getValue();

      // eventTime 기준으로 최신 이벤트 찾기
      EventHandled finalEvent = businessKeyEvents.stream()
          .max((e1, e2) -> {
            ZonedDateTime eventTime1 = e1.getEventTime() != null ? e1.getEventTime() : ZonedDateTime.parse("1970-01-01T00:00:00Z");
            ZonedDateTime eventTime2 = e2.getEventTime() != null ? e2.getEventTime() : ZonedDateTime.parse("1970-01-01T00:00:00Z");
            return eventTime1.compareTo(eventTime2);
          })
          .orElse(businessKeyEvents.get(0));

      result.put(finalEvent.getEventId() + ":" + businessKey, finalEvent);

      log.debug("Final state for businessKey {}: {} (eventTime: {})",
          businessKey, finalEvent.getEventType(), finalEvent.getEventTime());
    }

    return result;
  }


  /**
   * bucketTime 기준 10분 간격 좋아요수 누적
   *
   * @param productId  상품 ID
   * @param bucketTime 버킷 시간 (yyyyMMddHHmm 형식)
   */
  private void incrementLikeCountByBucketTime(Long productId, String bucketTime) {
    try {
      String redisKey = String.format("product_likes:%d:%s", productId, bucketTime);

      Long likeCount = redisTemplate.opsForValue().increment(redisKey);
      redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

      log.debug("Product like count incremented: productId={}, bucketTime={}, count={}",
          productId, bucketTime, likeCount);

    } catch (Exception e) {
      log.error("Failed to increment like count by bucket time: productId={}, bucketTime={}",
          productId, bucketTime, e);
    }
  }

  /**
   * bucketTime 기준 10분 간격 좋아요수 감소
   *
   * @param productId  상품 ID
   * @param bucketTime 버킷 시간 (yyyyMMddHHmm 형식)
   */
  private void decrementLikeCountByBucketTime(Long productId, String bucketTime) {
    try {
      String redisKey = String.format("product_likes:%d:%s", productId, bucketTime);

      Long likeCount = redisTemplate.opsForValue().decrement(redisKey);
      redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

      log.debug("Product like count decremented: productId={}, bucketTime={}, count={}",
          productId, bucketTime, likeCount);

    } catch (Exception e) {
      log.error("Failed to decrement like count by bucket time: productId={}, bucketTime={}",
          productId, bucketTime, e);
    }
  }

  /**
   * 10분 단위 버킷 시간 생성 (eventTime 기준)
   *
   * @param eventTime ZonedDateTime 이벤트 시간
   * @return 10분 단위로 버킷팅된 LocalDateTime
   */
  private LocalDateTime getBucketTime(ZonedDateTime eventTime) {
    try {
      if (eventTime == null) {
        LocalDateTime now = LocalDateTime.now();
        int bucketMinutes = (now.getMinute() / 10) * 10;
        return now.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
      }
      LocalDateTime eventDateTime = eventTime.toLocalDateTime();
      int bucketMinutes = (eventDateTime.getMinute() / 10) * 10;
      return eventDateTime.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
    } catch (Exception e) {
      log.warn("Failed to parse eventTime, using current time: eventTime={}", eventTime, e);
      LocalDateTime now = LocalDateTime.now();
      int bucketMinutes = (now.getMinute() / 10) * 10;
      return now.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
    }
  }

  /**
   * LocalDateTime을 yyyyMMddHHmm 문자열로 변환
   */
  private String getBucketTimeKey(LocalDateTime bucketTime) {
    if (bucketTime == null) {
      return null;
    }
    return bucketTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
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
          log.info("Marked obsolete like event as completed: eventId={}, businessKey={}, id={}",
              event.getEventId(), event.getBusinessKey(), event.getId());
        } catch (Exception e) {
          log.warn("Failed to mark obsolete like event as completed: eventId={}",
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
      log.info("Product cache evicted for like event: productId={}", productId);
    } catch (Exception e) {
      log.warn("Failed to evict product cache for like event: productId={}, error={}",
          productId, e.getMessage());
    }
  }
}
