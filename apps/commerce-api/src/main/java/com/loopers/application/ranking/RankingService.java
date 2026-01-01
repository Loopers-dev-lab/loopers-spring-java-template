package com.loopers.application.ranking;

import com.loopers.domain.ranking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class RankingService {

  private final RedisTemplate<String, String> redisTemplate;
  private final MvProductRankWeeklyService mvWeeklyService;
  private final MvProductRankMonthlyService mvMonthlyService;
  private final ProductMetricsDailyService dailyService;

  public Integer getProductRank(Long productId) {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String rankingKey = "ranking:all:" + date;
    Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, productId.toString());

    // rank가 null이면 랭킹에 없는 상품
    return rank != null ? rank.intValue() + 1 : null; // 0부터 시작하므로 +1
  }

  /**
   * 특정 날짜의 전체 랭킹에서 상품의 순위를 조회
   *
   * @param date      조회할 날짜 (yyyyMMdd 형식)
   * @param productId 상품 ID
   * @return 랭킹 순위 (1부터 시작), 랭킹에 없으면 null
   */
  public Integer getProductRank(String date, Long productId) {
    String rankingKey = "ranking:all:" + date;
    Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, productId.toString());

    // rank가 null이면 랭킹에 없는 상품
    return rank != null ? rank.intValue() + 1 : null; // 0부터 시작하므로 +1
  }

  /**
   * 특정 날짜의 전체 랭킹에서 상품의 점수를 조회
   *
   * @param date      조회할 날짜 (yyyyMMdd 형식)
   * @param productId 상품 ID
   * @return 랭킹 점수, 랭킹에 없으면 null
   */
  public Double getProductScore(String date, Long productId) {
    String rankingKey = "ranking:all:" + date;
    return redisTemplate.opsForZSet().score(rankingKey, productId.toString());
  }

  /**
   * 특정 날짜의 전체 랭킹 총 개수 조회
   *
   * @param date 조회할 날짜 (yyyyMMdd 형식)
   * @return 랭킹에 있는 총 상품 개수
   */
  public Long getTotalRankingCount(String date) {
    String rankingKey = "ranking:all:" + date;
    return redisTemplate.opsForZSet().zCard(rankingKey);
  }



  private List<Long> getWeeklyRankingFromCache(String date, Pageable pageable) {
    String rankingKey = "ranking:weekly:" + date;
    return getRankingFromCache(rankingKey, pageable);
  }


  private String convertDateToYearMonthWeek(String date) {
    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
    WeekFields weekFields = WeekFields.of(Locale.getDefault());
    int weekOfYear = localDate.get(weekFields.weekOfYear());
    return localDate.format(DateTimeFormatter.ofPattern("yyyy")) + String.format("%02d", weekOfYear);
  }


  /**
   * date 파라미터를 분석하여 기간별 랭킹 조회
   *
   * @param date     날짜 정보 (YYYYMMDD, YYYYWWW, YYYYMMM 형식)
   * @param pageable 페이징 정보
   * @return 랭킹 순서대로 정렬된 상품 ID 목록
   */
  public List<Long> getRankingProductIdsByDate(String date, Pageable pageable) {
    RankingDateParser parser = RankingDateParser.parse(date);

    switch (parser.getPeriod()) {
      case DAILY -> {
        return getDailyRankingProductIds(parser.getConvertedDate(), pageable);
      }
      case WEEKLY -> {
        return getWeeklyRankingProductIdsWithMV(parser.getConvertedDate(), parser.getPeriodKey(), pageable);
      }
      case MONTHLY -> {
        return getMonthlyRankingProductIdsWithMV(parser.getConvertedDate(), parser.getPeriodKey(), pageable);
      }
      default -> throw new IllegalArgumentException("지원하지 않는 기간입니다: " + parser.getPeriod());
    }
  }

  /**
   * date 파라미터를 분석하여 기간별 랭킹 총 개수 조회
   */
  public Long getTotalRankingCountByDate(String date) {
    RankingDateParser parser = RankingDateParser.parse(date);

    switch (parser.getPeriod()) {
      case DAILY -> {
        return getTotalRankingCount(parser.getConvertedDate());
      }
      case WEEKLY -> {
        return getTotalWeeklyRankingCountWithMV(parser.getConvertedDate(), parser.getPeriodKey());
      }
      case MONTHLY -> {
        return getTotalMonthlyRankingCountWithMV(parser.getConvertedDate(), parser.getPeriodKey());
      }
      default -> throw new IllegalArgumentException("지원하지 않는 기간입니다: " + parser.getPeriod());
    }
  }

  /**
   * 기간별 랭킹에서 상품 ID 목록을 조회 (캐시 우선, MV fallback, DB fallback)
   *
   * @param period   조회 기간 (daily, weekly, monthly)
   * @param date     조회할 날짜 (yyyyMMdd)
   * @param pageable 페이징 정보
   * @return 랭킹 순서대로 정렬된 상품 ID 목록
   */
  public List<Long> getRankingProductIdsByPeriod(RankingPeriod period, String date, Pageable pageable) {
    switch (period) {
      case DAILY -> {
        return getDailyRankingProductIds(date, pageable);
      }
      case WEEKLY -> {
        return getWeeklyRankingProductIdsWithMV(date, pageable);
      }
      case MONTHLY -> {
        return getMonthlyRankingProductIdsWithMV(date, pageable);
      }
      default -> throw new IllegalArgumentException("지원하지 않는 기간입니다: " + period);
    }
  }

  /**
   * 기간별 랭킹 총 개수 조회
   */
  public Long getTotalRankingCountByPeriod(RankingPeriod period, String date) {
    switch (period) {
      case DAILY -> {
        return getTotalRankingCount(date);
      }
      case WEEKLY -> {
        return getTotalWeeklyRankingCountWithMV(date);
      }
      case MONTHLY -> {
        return getTotalMonthlyRankingCountWithMV(date);
      }
      default -> throw new IllegalArgumentException("지원하지 않는 기간입니다: " + period);
    }
  }

  private List<Long> getDailyRankingProductIds(String date, Pageable pageable) {
    String rankingKey = "ranking:all:" + date;

    long start = (long) pageable.getPageNumber() * pageable.getPageSize();
    long end = start + pageable.getPageSize() - 1;

    Set<String> rankedProductIds = redisTemplate.opsForZSet().reverseRange(rankingKey, start, end);

    if (rankedProductIds == null || rankedProductIds.isEmpty()) {
      return List.of();
    }

    return rankedProductIds.stream()
        .map(Long::parseLong)
        .toList();
  }

  private List<Long> getWeeklyRankingProductIdsWithMV(String date, Pageable pageable) {
    // 1. Redis 캐시에서 조회 시도
    List<Long> cachedProductIds = getWeeklyRankingFromCache(date, pageable);
    if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
      log.debug("Weekly ranking found in cache for date: {}, size: {}", date, cachedProductIds.size());
      return cachedProductIds;
    }

    // 2. MV에서 조회 시도 (캐시 miss 시)
    String yearMonthWeek = convertDateToYearMonthWeek(date);
    if (mvWeeklyService.existsByYearMonthWeek(yearMonthWeek)) {
      log.debug("Weekly ranking found in MV for period: {}", yearMonthWeek);
      return mvWeeklyService.getWeeklyRankingProductIds(yearMonthWeek, pageable);
    }

    // 3. ProductMetricsDaily에서 집계하여 Redis에 저장 후 반환 (MV miss 시)
    log.warn("Weekly ranking MV miss for period: {}, falling back to ProductMetricsDaily aggregation", yearMonthWeek);
    return dailyService.calculateAndCacheWeeklyRanking(date, yearMonthWeek, pageable);
  }

  private List<Long>  getWeeklyRankingProductIdsWithMV(String date, String periodKey, Pageable pageable) {
    // 1. Redis 캐시에서 조회 시도
    List<Long> cachedProductIds = getWeeklyRankingFromCache(date, pageable);
    if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
      log.debug("Weekly ranking found in cache for date: {}, size: {}", date, cachedProductIds.size());
      return cachedProductIds;
    }

    // 2. MV에서 조회 시도 (캐시 miss 시)
    if (mvWeeklyService.existsByYearMonthWeek(periodKey)) {
      log.debug("Weekly ranking found in MV for period: {}", periodKey);
      return mvWeeklyService.getWeeklyRankingProductIds(periodKey, pageable);
    }

    // 3. ProductMetricsDaily에서 집계하여 Redis에 저장 후 반환 (MV miss 시)
    log.warn("Weekly ranking MV miss for period: {}, falling back to ProductMetricsDaily aggregation", periodKey);
    return dailyService.calculateAndCacheWeeklyRanking(date, periodKey, pageable);
  }

  private List<Long> getMonthlyRankingProductIdsWithMV(String date, Pageable pageable) {
    // 1. Redis 캐시에서 조회 시도
    String rankingKey = "ranking:monthly:" + date;
    List<Long> cachedProductIds = getRankingFromCache(rankingKey, pageable);
    if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
      log.debug("Monthly ranking found in cache for date: {}, size: {}", date, cachedProductIds.size());
      return cachedProductIds;
    }

    // 2. MV에서 조회 시도 (캐시 miss 시)
    String yearMonth = convertDateToYearMonth(date);
    if (mvMonthlyService.existsByYearMonth(yearMonth)) {
      log.debug("Monthly ranking found in MV for period: {}", yearMonth);
      return mvMonthlyService.getMonthlyRankingProductIds(yearMonth, pageable);
    }

    // 3. ProductMetricsDaily에서 집계하여 Redis에 저장 후 반환 (MV miss 시)
    log.warn("Monthly ranking MV miss for period: {}, falling back to ProductMetricsDaily aggregation", yearMonth);
    return dailyService.calculateAndCacheMonthlyRanking(date, yearMonth, pageable);
  }

  private List<Long> getMonthlyRankingProductIdsWithMV(String date, String periodKey, Pageable pageable) {
    // 1. Redis 캐시에서 조회 시도
    String rankingKey = "ranking:monthly:" + date;
    List<Long> cachedProductIds = getRankingFromCache(rankingKey, pageable);
    if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
      log.debug("Monthly ranking found in cache for date: {}, size: {}", date, cachedProductIds.size());
      return cachedProductIds;
    }

    // 2. MV에서 조회 시도 (캐시 miss 시)
    if (mvMonthlyService.existsByYearMonth(periodKey)) {
      log.debug("Monthly ranking found in MV for period: {}", periodKey);
      return mvMonthlyService.getMonthlyRankingProductIds(periodKey, pageable);
    }

    // 3. ProductMetricsDaily에서 집계하여 Redis에 저장 후 반환 (MV miss 시)
    log.warn("Monthly ranking MV miss for period: {}, falling back to ProductMetricsDaily aggregation", periodKey);
    return dailyService.calculateAndCacheMonthlyRanking(date, periodKey, pageable);
  }

  private Long getTotalWeeklyRankingCountWithMV(String date) {
    String rankingKey = "ranking:weekly:" + date;
    Long cacheCount = redisTemplate.opsForZSet().zCard(rankingKey);

    if (cacheCount != null && cacheCount > 0) {
      return cacheCount;
    }

    String yearMonthWeek = convertDateToYearMonthWeek(date);
    if (mvWeeklyService.existsByYearMonthWeek(yearMonthWeek)) {
      return mvWeeklyService.getWeeklyRankingCount(yearMonthWeek);
    }

    // fallback: ProductMetricsDaily에서 계산
    return 0L; // 실제 구현 시 ProductMetricsDaily 기반 카운트 로직 추가 필요
  }

  private Long getTotalWeeklyRankingCountWithMV(String date, String periodKey) {
    String rankingKey = "ranking:weekly:" + date;
    Long cacheCount = redisTemplate.opsForZSet().zCard(rankingKey);

    if (cacheCount != null && cacheCount > 0) {
      return cacheCount;
    }

    if (mvWeeklyService.existsByYearMonthWeek(periodKey)) {
      return mvWeeklyService.getWeeklyRankingCount(periodKey);
    }

    // fallback: ProductMetricsDaily에서 계산
    return 0L; // 실제 구현 시 ProductMetricsDaily 기반 카운트 로직 추가 필요
  }

  private Long getTotalMonthlyRankingCountWithMV(String date) {
    String rankingKey = "ranking:monthly:" + date;
    Long cacheCount = redisTemplate.opsForZSet().zCard(rankingKey);

    if (cacheCount != null && cacheCount > 0) {
      return cacheCount;
    }

    String yearMonth = convertDateToYearMonth(date);
    if (mvMonthlyService.existsByYearMonth(yearMonth)) {
      return mvMonthlyService.getMonthlyRankingCount(yearMonth);
    }

    return 0L; // MV가 없으면 0 반환
  }

  private Long getTotalMonthlyRankingCountWithMV(String date, String periodKey) {
    String rankingKey = "ranking:monthly:" + date;
    Long cacheCount = redisTemplate.opsForZSet().zCard(rankingKey);

    if (cacheCount != null && cacheCount > 0) {
      return cacheCount;
    }

    if (mvMonthlyService.existsByYearMonth(periodKey)) {
      return mvMonthlyService.getMonthlyRankingCount(periodKey);
    }

    return 0L; // MV가 없으면 0 반환
  }

  private List<Long> getRankingFromCache(String rankingKey, Pageable pageable) {
    long start = (long) pageable.getPageNumber() * pageable.getPageSize();
    long end = start + pageable.getPageSize() - 1;

    Set<String> rankedProductIds = redisTemplate.opsForZSet().reverseRange(rankingKey, start, end);

    if (rankedProductIds == null || rankedProductIds.isEmpty()) {
      return null;
    }

    return rankedProductIds.stream()
        .map(Long::parseLong)
        .toList();
  }

  private String convertDateToYearMonth(String date) {
    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
    return localDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
  }

}
