package com.loopers.batch.interfaces;

import com.loopers.batch.application.BatchJobFacade;
import com.loopers.batch.domain.ranking.RankingPeriod;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

  private final BatchJobFacade batchJobFacade;

  /**
   * 주간 랭킹 집계: 매주 월요일 02:00 실행 (전주 월~일 데이터 집계)
   */
  @Scheduled(cron = "0 0 2 * * MON")
  public void runWeeklyRankingAggregation() {
    LocalDate lastWeek = LocalDate.now().minusWeeks(1);
    log.info("주간 랭킹 집계 스케줄 실행: baseDate={}", lastWeek);
    batchJobFacade.runRankingAggregation(RankingPeriod.WEEKLY, lastWeek);
  }

  /**
   * 월간 랭킹 집계: 매월 1일 03:00 실행 (전월 1일~말일 데이터 집계)
   */
  @Scheduled(cron = "0 0 3 1 * *")
  public void runMonthlyRankingAggregation() {
    LocalDate lastMonth = LocalDate.now().minusMonths(1);
    log.info("월간 랭킹 집계 스케줄 실행: baseDate={}", lastMonth);
    batchJobFacade.runRankingAggregation(RankingPeriod.MONTHLY, lastMonth);
  }
}
