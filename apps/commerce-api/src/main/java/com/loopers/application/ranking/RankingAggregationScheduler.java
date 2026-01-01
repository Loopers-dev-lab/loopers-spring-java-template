package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * 주간, 월간 랭킹 스케줄러
 * 매일 자정에 일간 집계 완료 후 주간, 월간 랭킹을 갱신합니다.
 * 스프링 배치를 사용하여 tb_product_metrics의 일간 데이터를 집계하고,
 * 주간 및 월간 랭킹을 계산합니다.
 * 이 작업은 Chunk 단위로 처리하여 대량의 데이터를 효율적으로 처리합니다.
 * spring batch read process write 패턴을 따릅니다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingAggregationScheduler {

    private final RankingService rankingService;

    /**
     * 매일 자정에 실행 (일간 집계 완료 후)
     * 주간, 월간 랭킹 갱신
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void aggregateWeeklyAndMonthlyRankings() {
    }

}
