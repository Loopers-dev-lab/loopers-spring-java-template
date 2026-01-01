package com.loopers.application.ranking;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.infrastructure.ranking.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductMetricsDailyService {

  private final ProductMetricsDailyRepository productMetricsDailyRepository;
  private final RedisTemplate<String, String> redisTemplate;

  /**
   * 특정 날짜의 일간 메트릭 데이터 존재 여부 확인
   */
  public boolean existsByYearMonthDay(String yearMonthDay) {
    return productMetricsDailyRepository.existsByYearMonthDay(yearMonthDay);
  }

  /**
   * ProductMetricsDaily에서 주간 데이터를 집계하여 Redis에 저장 후 반환
   */
  public List<Long> calculateAndCacheWeeklyRanking(String date, String yearMonthWeek, Pageable pageable) {
    // 주간 날짜 범위 계산 (해당 주의 월요일부터 일요일까지)
    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
    LocalDate weekStart = localDate.minusDays(localDate.getDayOfWeek().getValue() - 1);
    LocalDate weekEnd = weekStart.plusDays(6);

    String startDate = weekStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String endDate = weekEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    log.info("Week range: {} to {}", startDate, endDate);
    System.out.print(startDate + endDate);

    // ProductMetricsDaily에서 주간 집계 데이터 조회
    Page<ProductMetricsWeekly> weeklyMetrics = productMetricsDailyRepository
        .findWeeklyAggregateByDateRange(startDate, endDate, yearMonthWeek, Pageable.unpaged());
    System.out.print(weeklyMetrics.getTotalElements());
    if (weeklyMetrics.hasContent()) {
      // Redis에 랭킹 데이터 저장
      String rankingKey = "ranking:weekly:" + date;
      cacheWeeklyRankingToRedis(rankingKey, weeklyMetrics.getContent());

      // 요청된 페이지만큼 반환
      return weeklyMetrics.getContent().stream()
          .skip((long) pageable.getPageNumber() * pageable.getPageSize())
          .limit(pageable.getPageSize())
          .map(ProductMetricsWeekly::getProductId)
          .toList();
    }

    return List.of();
  }

  /**
   * ProductMetricsDaily에서 월간 데이터를 집계하여 Redis에 저장 후 반환
   */
  public List<Long> calculateAndCacheMonthlyRanking(String date, String yearMonth, Pageable pageable) {
    String yearMonthPattern = yearMonth + "%";

    // ProductMetricsDaily에서 월간 집계 데이터 조회
    Page<ProductMetricsMonthly> monthlyMetrics = productMetricsDailyRepository
        .findMonthlyAggregateByYearMonth(yearMonthPattern, yearMonth, Pageable.unpaged());

    if (monthlyMetrics.hasContent()) {
      // Redis에 랭킹 데이터 저장
      String rankingKey = "ranking:monthly:" + date;
      cacheMonthlyRankingToRedis(rankingKey, monthlyMetrics.getContent());

      // 요청된 페이지만큼 반환
      return monthlyMetrics.getContent().stream()
          .skip((long) pageable.getPageNumber() * pageable.getPageSize())
          .limit(pageable.getPageSize())
          .map(ProductMetricsMonthly::getProductId)
          .toList();
    }

    return List.of();
  }

  /**
   * 주간 랭킹 데이터를 Redis에 캐시
   */
  private void cacheWeeklyRankingToRedis(String rankingKey, List<ProductMetricsWeekly> weeklyMetrics) {
    try {
      // 기존 데이터 삭제
      redisTemplate.delete(rankingKey);

      // 새로운 랭킹 데이터 저장
      for (ProductMetricsWeekly metric : weeklyMetrics) {
        double score = metric.getLikeCount() * 3.0 + metric.getOrderCount() * 10.0 + metric.getViewCount() * 1.0;
        redisTemplate.opsForZSet().add(rankingKey, metric.getProductId().toString(), score);
      }

      // TTL 설정 (7일)
      redisTemplate.expire(rankingKey, java.time.Duration.ofDays(7));

      log.info("Cached {} weekly ranking items to Redis key: {}", weeklyMetrics.size(), rankingKey);
    } catch (Exception e) {
      log.error("Failed to cache weekly ranking to Redis: {}", rankingKey, e);
    }
  }

  /**
   * 월간 랭킹 데이터를 Redis에 캐시
   */
  private void cacheMonthlyRankingToRedis(String rankingKey, List<ProductMetricsMonthly> monthlyMetrics) {
    try {
      // 기존 데이터 삭제
      redisTemplate.delete(rankingKey);

      // 새로운 랭킹 데이터 저장
      for (ProductMetricsMonthly metric : monthlyMetrics) {
        double score = metric.getLikeCount() * 3.0 + metric.getOrderCount() * 10.0 + metric.getViewCount() * 1.0;
        redisTemplate.opsForZSet().add(rankingKey, metric.getProductId().toString(), score);
      }

      // TTL 설정 (30일)
      redisTemplate.expire(rankingKey, java.time.Duration.ofDays(30));

      log.info("Cached {} monthly ranking items to Redis key: {}", monthlyMetrics.size(), rankingKey);
    } catch (Exception e) {
      log.error("Failed to cache monthly ranking to Redis: {}", rankingKey, e);
    }
  }
}
