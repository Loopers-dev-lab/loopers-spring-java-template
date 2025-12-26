package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 랭킹 스케줄러
 * - 1분 주기: 시간별 슬라이딩 윈도우 재구성 (최근 1시간)
 * - 10분 주기: 일간 슬라이딩 윈도우 재구성 (최근 24시간)
 * - 1시간 주기: 스냅샷 집계 및 Redis 동기화
 * - 매일 자정: Daily 스냅샷 생성
 * - 매일 23:55: 파티션 관리 (내일 파티션 생성, 일주일 전 파티션 삭제)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingSnapshotService rankingSnapshotService;
    private final RankingEventLogPartitionService rankingEventLogPartitionService;

    /**
     * 1분마다 실행: 시간별 슬라이딩 윈도우 재구성
     * 최근 1시간 데이터를 DB에서 집계하여 Redis ranking:hourly 키를 교체
     */
    @Scheduled(cron = "0 * * * * *")
    public void rebuildHourlyRanking() {
        log.debug("Starting hourly ranking rebuild (sliding window)...");
        
        try {
            rankingSnapshotService.rebuildHourlyRanking();
            log.debug("Hourly ranking rebuild completed successfully");
        } catch (Exception e) {
            log.error("Failed to rebuild hourly ranking", e);
            // 실패해도 다음 주기에 재시도되므로 예외만 로깅
        }
    }

    /**
     * 10분마다 실행: 일간 슬라이딩 윈도우 재구성
     * 최근 24시간 데이터를 DB에서 집계하여 Redis ranking:daily 키를 교체
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void rebuildDailyRanking() {
        log.info("Starting daily ranking rebuild (sliding window)...");
        
        try {
            rankingSnapshotService.rebuildDailyRanking();
            log.info("Daily ranking rebuild completed successfully");
        } catch (Exception e) {
            log.error("Failed to rebuild daily ranking", e);
            // 실패해도 다음 주기에 재시도되므로 예외만 로깅
        }
    }

    /**
     * 1시간마다 실행: 스냅샷 집계 및 Redis 동기화
     * 매 시간 정각(0분)에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
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
     * 매일 23:55에 실행: RankingEventLog 파티션 관리
     * - 내일 파티션 생성 (자정 직후 로그 대량 발생 대비)
     * - 일주일 전 파티션 삭제
     */
    @Scheduled(cron = "0 55 23 * * *")
    public void managePartitions() {
        log.info("Starting partition management for RankingEventLog...");
        
        try {
            // 내일 파티션 생성
            rankingEventLogPartitionService.createNextDayPartition();
            
            // 일주일 전 파티션 삭제
            rankingEventLogPartitionService.dropOldPartitions();
            
            log.info("Partition management completed successfully");
        } catch (Exception e) {
            log.error("Failed to manage partitions", e);
            // 실패 시 알림 발송 등 추가 처리 가능
        }
    }

}



