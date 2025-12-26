package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 랭킹 스케줄러
 * - 1분 주기: 시간별 슬라이딩 윈도우 재구성 (최근 1시간)
 * - 10분 주기: 일간 슬라이딩 윈도우 재구성 (최근 24시간)
 * - 1시간 주기: Hourly 및 Daily 스냅샷 생성
 *   - Hourly: 주/월별 집계를 위한 확정 데이터 (특정 시간대의 1시간 데이터)
 *   - Daily: Fallback을 위한 슬라이딩 윈도우 데이터 (최근 24시간)
 * - 매일 자정: Daily 스냅샷 생성 (어제의 Hourly 스냅샷에서 추출 - 날짜별 확정 데이터)
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
     * 1시간마다 실행: Hourly 및 Daily 스냅샷 생성
     * 매 시간 정각(0분)에 이전 시간대의 스냅샷 생성
     * - Hourly: 주/월별 랭킹 집계를 위한 확정 데이터 저장
     * - Daily: 슬라이딩 윈도우용 Fallback 데이터 저장 (최근 24시간)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void createHourlySnapshot() {
        log.info("Starting hourly and daily snapshot creation...");
        
        try {
            // 이전 시간대의 스냅샷 생성 (예: 10:00에 실행되면 9:00 스냅샷 생성)
            LocalDateTime previousHour = LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusHours(1);
            
            // Hourly 스냅샷 생성 (특정 시간대의 1시간 데이터)
            rankingSnapshotService.createHourlySnapshot(previousHour);
            log.info("Hourly snapshot creation completed successfully for time: {}", previousHour);
            
            // Daily 스냅샷 생성 (최근 24시간 슬라이딩 윈도우)
            rankingSnapshotService.createDailySnapshot(previousHour);
            log.info("Daily snapshot creation completed successfully for time: {} (24-hour sliding window)", previousHour);
        } catch (Exception e) {
            log.error("Failed to create hourly/daily snapshot", e);
            // 실패 시 알림 발송 등 추가 처리 가능
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



