package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * 주간, 월간 랭킹 스케줄러
 * <p>
 * 매일 자정에 일간 집계 완료 후 주간, 월간 랭킹을 갱신합니다.
 * 스프링 배치를 사용하여 tb_product_metrics_daily의 일간 데이터를 집계하고,
 * 주간 및 월간 랭킹을 계산합니다.
 * 이 작업은 Chunk 단위로 처리하여 대량의 데이터를 효율적으로 처리합니다.
 * spring batch read process write 패턴을 따릅니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingAggregationScheduler {

    private final RankingAggregationService rankingAggregationService;

    /**
     * 매일 자정에 실행 (일간 집계 완료 후)
     * 주간, 월간 랭킹 갱신
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void aggregateWeeklyAndMonthlyRankings() {
        try {
            log.info("스케줄러: 주간 및 월간 랭킹 집계 배치 실행 시작");

            // 어제 날짜 기준으로 집계 (오늘 자정에 실행되므로 어제 데이터까지 집계)
            ZonedDateTime targetDate = ZonedDateTime.now().minusDays(1);

            rankingAggregationService.executeWeeklyAndMonthlyRanking(targetDate);

            log.info("스케줄러: 주간 및 월간 랭킹 집계 배치 실행 완료");

        } catch (Exception e) {
            log.error("스케줄러: 주간 및 월간 랭킹 집계 배치 실행 실패", e);
            // 스케줄러는 예외를 던지지 않고 로그만 남김 (다음 실행에 영향 주지 않음)
        }
    }
}
