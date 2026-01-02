package com.loopers.application.rank;

import com.loopers.domain.rank.RankingService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

  private final RankingService rankingService;

  @Scheduled(cron = "0 50 23 * * *")
  public void scheduleRankingCarryOver() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String today = now.format(formatter);
    String tomorrow = now.plusDays(1).format(formatter);

    log.info("Starting Ranking Carry-Over: {} -> {}", today, tomorrow);

    try {
      rankingService.carryOverRanking(today, tomorrow, 0.1);
      log.info("Ranking Carry-Over completed successfully.");
    } catch (Exception e) {
      log.error("Ranking Carry-Over failed", e);
    }
  }
}
