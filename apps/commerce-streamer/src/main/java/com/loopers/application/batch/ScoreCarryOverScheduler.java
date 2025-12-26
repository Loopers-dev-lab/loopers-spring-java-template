package com.loopers.application.batch;

import com.loopers.domain.ranking.DailyRanking;
import com.loopers.domain.ranking.DailyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.loopers.support.util.RedisUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCarryOverScheduler {

  private final RedisTemplate<String, String> redisTemplate;
  private final DailyRankingRepository dailyRankingRepository;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  // 어제 점수 반영 비율 (20%)
  private static final double CARRY_OVER_RATIO = 0.2;
  // TTL 설정 (2일)
  private static final long TTL_DAYS = 2;

  /**
   * 23:50 - 일간 랭킹 확정 및 다음날 carry over
   */
  @Scheduled(cron = "0 50 23 * * *")
  public void dailyRankingCarryOver() {
    try {
      LocalDate today = LocalDate.now();
      LocalDate tomorrow = LocalDate.now().plusDays(1);

      String todayKey = "ranking:all:" + today.format(DATE_FORMATTER);
      String tomorrowKey = "ranking:all:" + tomorrow.format(DATE_FORMATTER);

      log.info("Starting daily ranking carry over for date: {}", today);

      // 1. 오늘 최종 → DB 저장 (영구 보존용)
      saveDailyRankingToDB(todayKey, today);

      // 2. 내일로 20% carry over
      carryOverToTomorrow(todayKey, tomorrowKey);

      log.info("Daily ranking carry over completed successfully");

    } catch (Exception e) {
      log.error("Failed to execute daily ranking carry over", e);
    }
  }

  private void saveDailyRankingToDB(String rankingKey, LocalDate rankingDate) {
    Set<ZSetOperations.TypedTuple<String>> rankedProducts =
        redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, -1);

    if (rankedProducts == null || rankedProducts.isEmpty()) {
      log.info("No ranking data to save for date: {}", rankingDate);
      return;
    }

    List<DailyRanking> dailyRankings = new ArrayList<>();
    int position = 1;

    for (var tuple : rankedProducts) {
      String productId = tuple.getValue();
      Double score = tuple.getScore();
      if (score != null) {
        DailyRanking dailyRanking = DailyRanking.of(
            rankingDate,
            Long.parseLong(productId),
            position,
            score
        );
        dailyRankings.add(dailyRanking);
        position++;
      }
    }

    dailyRankingRepository.saveAll(dailyRankings);
    log.info("Saved {} daily rankings to DB for date: {}", dailyRankings.size(), rankingDate);
  }

  private void carryOverToTomorrow(String todayKey, String tomorrowKey) {
    Set<String> productIds = redisTemplate.opsForZSet().range(todayKey, 0, -1);

    if (productIds == null || productIds.isEmpty()) {
      log.info("No data to carry over to tomorrow");
      return;
    }

    int carriedOverCount = 0;
    double totalCarriedScore = 0;

    for (String productId : productIds) {
      Double todayScore = redisTemplate.opsForZSet().score(todayKey, productId);

      if (todayScore != null && todayScore > 0) {
        // 오늘 점수의 20%만 내일로 carry over
        double carriedScore = todayScore * CARRY_OVER_RATIO;
        redisTemplate.opsForZSet().incrementScore(tomorrowKey, productId, carriedScore);

        carriedOverCount++;
        totalCarriedScore += carriedScore;
      }
    }

    // carry over 완료 후 TTL 설정
    RedisUtils.setTtlInDaysIfNeeded(redisTemplate, tomorrowKey, TTL_DAYS);
    
    log.info("Carried over {} products to tomorrow. Total score: {} ({}% of today)",
        carriedOverCount, String.format("%.2f", totalCarriedScore), (int) (CARRY_OVER_RATIO * 100));
  }

  /**
   * 수동 실행을 위한 메서드 (테스트용)
   */
  public void executeCarryOver() {
    log.info("Manual carry over execution started");
    dailyRankingCarryOver();
  }

}
