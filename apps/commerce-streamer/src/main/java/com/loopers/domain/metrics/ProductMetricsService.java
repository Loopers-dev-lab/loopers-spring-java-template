package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

  private final ProductMetricsRepository productMetricsRepository;

  @Transactional
  public void batchUpdateViewCounts(Map<Long, Map<String, Long>> productBucketCounts) {
    for (Map.Entry<Long, Map<String, Long>> productEntry : productBucketCounts.entrySet()) {
      Long productId = productEntry.getKey();
      Map<String, Long> bucketCounts = productEntry.getValue();

      for (Map.Entry<String, Long> bucketEntry : bucketCounts.entrySet()) {
        String bucketTimeKey = bucketEntry.getKey();
        Long deltaCount = bucketEntry.getValue();

        try {
          int updatedRows = productMetricsRepository.incrementViewCountByDelta(productId, bucketTimeKey, deltaCount);

          if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTimeKey);
            for (int i = 0; i < deltaCount; i++) {
              metrics.incrementViewCount();
            }
            productMetricsRepository.save(metrics);
            log.debug("Created new ProductMetrics with delta view count: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          } else {
            log.debug("Updated view count by delta: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          }
        } catch (Exception e) {
          log.error("Failed to batch update view count: productId={}, bucketTimeKey={}, delta={}",
              productId, bucketTimeKey, deltaCount, e);
        }
      }
    }
  }

  @Transactional
  public void batchUpdateLikeCounts(Map<Long, Map<String, Long>> productBucketCounts) {
    for (Map.Entry<Long, Map<String, Long>> productEntry : productBucketCounts.entrySet()) {
      Long productId = productEntry.getKey();
      Map<String, Long> bucketCounts = productEntry.getValue();

      for (Map.Entry<String, Long> bucketEntry : bucketCounts.entrySet()) {
        String bucketTimeKey = bucketEntry.getKey();
        Long deltaCount = bucketEntry.getValue();

        try {
          int updatedRows = productMetricsRepository.incrementLikeCountByDelta(productId, bucketTimeKey, deltaCount);

          if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTimeKey);
            if (deltaCount > 0) {
              for (int i = 0; i < deltaCount; i++) {
                metrics.incrementLikeCount();
              }
            } else {
              // 음수인 경우 (좋아요 취소가 더 많은 경우)
              for (int i = 0; i < Math.abs(deltaCount); i++) {
                metrics.decrementLikeCount();
              }
            }
            productMetricsRepository.save(metrics);
            log.debug("Created new ProductMetrics with delta like count: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          } else {
            log.debug("Updated like count by delta: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          }
        } catch (Exception e) {
          log.error("Failed to batch update like count: productId={}, bucketTimeKey={}, delta={}",
              productId, bucketTimeKey, deltaCount, e);
        }
      }
    }
  }

  @Transactional
  public void batchUpdateSalesRevenues(Map<Long, Map<String, Long>> productBucketCounts) {
    for (Map.Entry<Long, Map<String, Long>> productEntry : productBucketCounts.entrySet()) {
      Long productId = productEntry.getKey();
      Map<String, Long> bucketCounts = productEntry.getValue();

      for (Map.Entry<String, Long> bucketEntry : bucketCounts.entrySet()) {
        String bucketTimeKey = bucketEntry.getKey();
        Long deltaCount = bucketEntry.getValue();

        try {
          int updatedRows = productMetricsRepository.incrementSalesRevenueByDelta(productId, bucketTimeKey, deltaCount);

          if (updatedRows == 0) {
            // 메트릭스가 없으면 새로 생성
            ProductMetrics metrics = new ProductMetrics(productId, bucketTimeKey);
            metrics.incrementSalesRevenue(deltaCount);
            productMetricsRepository.save(metrics);
            log.debug("Created new ProductMetrics with delta sales revenue: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          } else {
            log.debug("Updated sales revenue by delta: productId={}, bucketTimeKey={}, delta={}",
                productId, bucketTimeKey, deltaCount);
          }
        } catch (Exception e) {
          log.error("Failed to batch update sales revenue: productId={}, bucketTimeKey={}, delta={}",
              productId, bucketTimeKey, deltaCount, e);
        }
      }
    }
  }

  /**
   * 현재 시간을 10분 단위로 truncate한 bucketTime 생성
   * 예: 2024-12-19 14:35:22 -> 2024-12-19 14:30:00
   * 2024-12-19 14:42:15 -> 2024-12-19 14:40:00
   */
  private LocalDateTime getBucketTime() {
    LocalDateTime now = LocalDateTime.now();
    // 분을 10분 단위로 내림 (0, 10, 20, 30, 40, 50)
    int bucketMinutes = (now.getMinute() / 10) * 10;
    return now.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
  }

  /**
   * bucketTime 문자열을 LocalDateTime으로 파싱
   *
   * @param bucketTimeKey yyyyMMddHHmm 형태의 문자열
   * @return LocalDateTime
   */
  private LocalDateTime parseBucketTime(String bucketTimeKey) {
    // yyyyMMddHHmm 형태의 문자열을 LocalDateTime으로 변환
    String year = bucketTimeKey.substring(0, 4);
    String month = bucketTimeKey.substring(4, 6);
    String day = bucketTimeKey.substring(6, 8);
    String hour = bucketTimeKey.substring(8, 10);
    String minute = bucketTimeKey.substring(10, 12);

    return LocalDateTime.of(
        Integer.parseInt(year),
        Integer.parseInt(month),
        Integer.parseInt(day),
        Integer.parseInt(hour),
        Integer.parseInt(minute)
    );
  }
}
