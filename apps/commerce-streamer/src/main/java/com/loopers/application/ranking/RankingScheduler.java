package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 랭킹 스케줄러
 * - 10분 주기: 스냅샷 집계 및 Redis 동기화
 * - 매일 자정: 전날 점수 이월
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final ProductRankingService productRankingService;
    private final RankingSnapshotService rankingSnapshotService;

    /**
     * 10분마다 실행: 스냅샷 집계 및 Redis 동기화
     * 매 시간 0분, 10분, 20분, 30분, 40분, 50분에 실행
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void createSnapshotAndSync() {
        log.info("Starting snapshot creation and Redis sync...");
        
        try {
            rankingSnapshotService.createSnapshotAndSync();
            log.info("Snapshot creation and sync completed successfully");
        } catch (Exception e) {
            log.error("Failed to create snapshot and sync", e);
            // 실패 시 알림 발송 등 추가 처리 가능
        }
    }

    /**
     * 매일 자정 00:00에 실행: 어제의 Hourly 스냅샷을 합산하여 Daily 스냅샷 생성
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void createDailySnapshot() {
        log.info("Starting daily snapshot creation from hourly snapshots...");
        
        try {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            rankingSnapshotService.createDailySnapshotFromHourly(yesterday);
            log.info("Daily snapshot creation completed successfully");
        } catch (Exception e) {
            log.error("Failed to create daily snapshot", e);
        }
    }

    /**
     * 매일 자정 00:05에 실행: 전날 점수를 오늘 랭킹에 이월
     * Daily 스냅샷 생성 후에 실행하도록 5분 지연
     */
    @Scheduled(cron = "${ranking.carry-over.schedule:0 5 0 * * *}")
    public void initializeDailyRanking() {
        log.info("Starting daily ranking initialization with score carry-over...");
        
        try {
            // 전날 점수의 일부를 오늘 랭킹에 복사
            productRankingService.carryOverPreviousDayScore();
            
            log.info("Daily ranking initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize daily ranking", e);
            // 실패 시 알림 발송 등 추가 처리 가능
        }
    }

    /**
     * 매월 1일 자정 00:00에 실행: 지난달의 Daily 스냅샷을 합산하여 Monthly 스냅샷 생성
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void createMonthlySnapshot() {
        log.info("Starting monthly snapshot creation from daily snapshots...");
        
        try {
            java.time.YearMonth lastMonth = java.time.YearMonth.now().minusMonths(1);
            rankingSnapshotService.createMonthlySnapshotFromDaily(lastMonth);
            log.info("Monthly snapshot creation completed successfully");
        } catch (Exception e) {
            log.error("Failed to create monthly snapshot", e);
        }
    }

    /**
     * 매년 1월 1일 자정 00:00에 실행: 작년의 Monthly 스냅샷을 합산하여 Yearly 스냅샷 생성
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    public void createYearlySnapshot() {
        log.info("Starting yearly snapshot creation from monthly snapshots...");
        
        try {
            Integer lastYear = java.time.LocalDate.now().getYear() - 1;
            rankingSnapshotService.createYearlySnapshotFromMonthly(lastYear);
            log.info("Yearly snapshot creation completed successfully");
        } catch (Exception e) {
            log.error("Failed to create yearly snapshot", e);
        }
    }
}



