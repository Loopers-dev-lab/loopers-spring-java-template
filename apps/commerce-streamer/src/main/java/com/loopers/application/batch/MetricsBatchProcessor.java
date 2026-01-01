package com.loopers.application.batch;

import com.loopers.domain.metrics.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsBatchProcessor {

  private final ProductMetricsService productMetricsService;
  private final RedisTemplate<String, String> redisTemplate;

  @Scheduled(fixedDelay = 60000) // 1분마다 실행 (배치 집계)
  public void aggregateMetricsToDb() {
    try {
      // 조회수 집계
      aggregateViewCounts();
      
      // 좋아요수 집계
      aggregateLikeCounts();
      
      // 판매량 집계
      aggregateSalesCounts();
      
    } catch (Exception e) {
      log.error("Failed to aggregate metrics to DB", e);
    }
  }

  /**
   * Redis → DB 조회수 집계
   */
  private void aggregateViewCounts() {
    try {
      Set<String> keys = scanKeys("product_views:*");
      
      if (keys == null || keys.isEmpty()) {
        return;
      }
      
      Map<Long, Map<String, Long>> productBucketCounts = new HashMap<>();
      
      for (String key : keys) {
        try {
          // GETDEL로 원자적으로 값 읽기 + 삭제
          String countStr = redisTemplate.opsForValue().getAndDelete(key);
          Long count = Long.valueOf(countStr != null ? countStr : "0");
          
          if (count > 0) {
            // product_views:{productId}:{bucketTime} 파싱
            String[] parts = key.split(":");
            Long productId = Long.valueOf(parts[1]);
            String bucketTime = parts[2];
            
            productBucketCounts
                .computeIfAbsent(productId, k -> new HashMap<>())
                .put(bucketTime, count);
            
            log.debug("Aggregated view count: productId={}, bucketTime={}, count={}", 
                     productId, bucketTime, count);
          }
        } catch (Exception e) {
          log.warn("Failed to process Redis view key: {}", key, e);
        }
      }
      
      if (!productBucketCounts.isEmpty()) {
        // 배치로 DB 업데이트
        productMetricsService.batchUpdateViewCounts(productBucketCounts);
        log.info("Batch updated view counts for {} products", productBucketCounts.size());
      }
      
    } catch (Exception e) {
      log.error("Failed to aggregate view counts to DB", e);
    }
  }

  /**
   * Redis → DB 좋아요수 집계
   */
  private void aggregateLikeCounts() {
    try {
      Set<String> keys = scanKeys("product_likes:*");
      
      if (keys == null || keys.isEmpty()) {
        return;
      }
      
      Map<Long, Map<String, Long>> productBucketCounts = new HashMap<>();
      
      for (String key : keys) {
        try {
          // GETDEL로 원자적으로 값 읽기 + 삭제
          String countStr = redisTemplate.opsForValue().getAndDelete(key);
          Long count = Long.valueOf(countStr != null ? countStr : "0");
          
          if (count != 0) { // 좋아요는 음수도 가능 (좋아요 취소가 더 많은 경우)
            // product_likes:{productId}:{bucketTime} 파싱
            String[] parts = key.split(":");
            Long productId = Long.valueOf(parts[1]);
            String bucketTime = parts[2];
            
            productBucketCounts
                .computeIfAbsent(productId, k -> new HashMap<>())
                .put(bucketTime, count);
            
            log.debug("Aggregated like count: productId={}, bucketTime={}, count={}", 
                     productId, bucketTime, count);
          }
        } catch (Exception e) {
          log.warn("Failed to process Redis like key: {}", key, e);
        }
      }
      
      if (!productBucketCounts.isEmpty()) {
        // 배치로 DB 업데이트
        productMetricsService.batchUpdateLikeCounts(productBucketCounts);
        log.info("Batch updated like counts for {} products", productBucketCounts.size());
      }
      
    } catch (Exception e) {
      log.error("Failed to aggregate like counts to DB", e);
    }
  }

  /**
   * Redis → DB 판매량 집계
   */
  private void aggregateSalesCounts() {
    try {
      Set<String> keys = scanKeys("product_sales:*");
      
      if (keys == null || keys.isEmpty()) {
        return;
      }
      
      Map<Long, Map<String, Long>> productBucketCounts = new HashMap<>();
      
      for (String key : keys) {
        try {
          // GETDEL로 원자적으로 값 읽기 + 삭제
          String countStr = redisTemplate.opsForValue().getAndDelete(key);
          Long count = Long.valueOf(countStr != null ? countStr : "0");
          
          if (count > 0) {
            // product_sales:{productId}:{bucketTime} 파싱
            String[] parts = key.split(":");
            Long productId = Long.valueOf(parts[1]);
            String bucketTime = parts[2];
            
            productBucketCounts
                .computeIfAbsent(productId, k -> new HashMap<>())
                .put(bucketTime, count);
            
            log.debug("Aggregated sales count: productId={}, bucketTime={}, count={}", 
                     productId, bucketTime, count);
          }
        } catch (Exception e) {
          log.warn("Failed to process Redis sales key: {}", key, e);
        }
      }
      
      if (!productBucketCounts.isEmpty()) {
        // 배치로 DB 업데이트
        productMetricsService.batchUpdateSalesRevenues(productBucketCounts);
        log.info("Batch updated sales revenues for {} products", productBucketCounts.size());
      }
      
    } catch (Exception e) {
      log.error("Failed to aggregate sales counts to DB", e);
    }
  }

  /**
   * SCAN 명령을 사용하여 패턴에 매칭되는 키들을 조회
   * KEYS 대신 SCAN을 사용하여 Redis 서버 블로킹 방지
   */
  private Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    
    ScanOptions options = ScanOptions.scanOptions()
        .match(pattern)
        .count(1000) // 한 번에 스캔할 키 개수
        .build();
        
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        keys.add(cursor.next());
      }
    } catch (Exception e) {
      log.error("Failed to scan Redis keys with pattern: {}", pattern, e);
    }
    
    return keys;
  }
}