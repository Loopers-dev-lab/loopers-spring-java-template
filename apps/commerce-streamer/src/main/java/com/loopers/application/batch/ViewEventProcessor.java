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
public class ViewEventProcessor {

  private final EventHandledService eventHandledService;
  private final ProductMetricsService productMetricsService;
  private final ProductRankingService productRankingService;
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, String> redisTemplate;


  @Scheduled(fixedDelay = 5000) // 5초마다 실행 (실시간성 향상)
  public void processPendingViewEvents() {
    // 조회 이벤트만 조회
    List<EventHandled> viewEvents = eventHandledService.findPendingEventsByType("ProductViewed");

    if (viewEvents.isEmpty()) {
      return;
    }

    log.info("Processing {} view events", viewEvents.size());

    // 모든 이벤트 처리 (중복 제거 없음)
    for (EventHandled event : viewEvents) {
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
    ZonedDateTime eventTime = event.getEventTime();
    LocalDateTime bucketTime = getBucketTime(eventTime);
    String bucketTimeKey = getBucketTimeKey(bucketTime);

    log.info("Processing ProductViewed event: productId={}, userId={}, eventId={}, eventTime={}, bucketTime={}, bucketTimeKey={}",
        productId, userId, event.getEventId(), eventTime, bucketTime, bucketTimeKey);

    // eventTime 기준 10분 간격 조회수 누적 (Redis만 즉시 반영)
    incrementViewCountByBucketTime(productId, bucketTimeKey);

    // 랭킹 점수 추가
    productRankingService.addViewScore(productId);

    // 상품 캐시 삭제
    evictProductCache(productId);

    log.info("ProductViewed metrics and ranking updated: productId={}, eventId={}",
        productId, event.getEventId());
  }

  /**
   * bucketTime 기준 10분 간격 조회수 누적
   *
   * @param productId  상품 ID
   * @param bucketTime 버킷 시간 (yyyyMMddHHmm 형식)
   */
  private void incrementViewCountByBucketTime(Long productId, String bucketTime) {
    try {
      String redisKey = String.format("product_views:%d:%s", productId, bucketTime);

      Long viewCount = redisTemplate.opsForValue().increment(redisKey);
      redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

      log.debug("Product view count incremented: productId={}, bucketTime={}, count={}",
          productId, bucketTime, viewCount);

    } catch (Exception e) {
      log.error("Failed to increment view count by bucket time: productId={}, bucketTime={}",
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
        return null;
      }
      LocalDateTime eventDateTime = eventTime.toLocalDateTime();
      int bucketMinutes = (eventDateTime.getMinute() / 10) * 10;
      return eventDateTime.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
    } catch (Exception e) {
      log.warn("Failed to parse eventTime, using current time: eventTime={}", eventTime, e);
      return null;
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
