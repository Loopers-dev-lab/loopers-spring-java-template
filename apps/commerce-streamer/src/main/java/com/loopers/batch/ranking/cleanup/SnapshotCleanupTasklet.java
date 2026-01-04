package com.loopers.batch.ranking.cleanup;

import com.loopers.batch.ranking.RankingType;
import com.loopers.infrastructure.ranking.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스냅샷 정리 Tasklet (랭킹 타입별)
 * 
 * 불필요한 중간 스냅샷 삭제 (정시/일시작/주시작/월시작만 보존)
 */
@Slf4j
public class SnapshotCleanupTasklet implements Tasklet {

    private final RankingType rankingType;
    private final RankingSnapshotHourlyJpaRepository hourlySnapshotRepository;
    private final RankingSnapshotDailyJpaRepository dailySnapshotRepository;
    private final RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository;
    private final RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SnapshotCleanupTasklet(RankingType rankingType,
                                  RankingSnapshotHourlyJpaRepository hourlySnapshotRepository,
                                  RankingSnapshotDailyJpaRepository dailySnapshotRepository,
                                  RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository,
                                  RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository) {
        this.rankingType = rankingType;
        this.hourlySnapshotRepository = hourlySnapshotRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.weeklySnapshotRepository = weeklySnapshotRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("SnapshotCleanupTasklet 시작: type={}", rankingType);

        try {
            int deletedCount = cleanupSnapshots();

            log.info("SnapshotCleanupTasklet 완료: type={}, deleted={}건", rankingType, deletedCount);
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            log.error("SnapshotCleanupTasklet 실패: type={}", rankingType, e);
            throw e;
        }
    }

    /**
     * 스냅샷 정리 실행
     */
    private int cleanupSnapshots() {
        CleanupConfig config = getCleanupConfig();
        
        String sql = "DELETE FROM " + config.tableName() + " WHERE " + config.whereCondition();
        Query query = entityManager.createNativeQuery(sql);
        
        int deleted = query.executeUpdate();
        log.info("{} 스냅샷 정리: {}건 삭제 ({})", rankingType, deleted, config.description());
        return deleted;
    }

    /**
     * 랭킹 타입별 정리 설정 조회
     */
    private CleanupConfig getCleanupConfig() {
        return switch (rankingType) {
            case HOURLY -> new CleanupConfig(
                "ranking_snapshot_hourly",
                "HOUR(snapshot_time) != 0 OR MINUTE(snapshot_time) != 0",
                "정시 00분만 보존"
            );
            case DAILY -> new CleanupConfig(
                "ranking_snapshot_daily",
                "HOUR(snapshot_time) != 0 OR MINUTE(snapshot_time) != 0",
                "00:00만 보존"
            );
            case WEEKLY -> new CleanupConfig(
                "ranking_snapshot_weekly",
                "DAYOFWEEK(snapshot_time) != 2 OR HOUR(snapshot_time) != 0 OR MINUTE(snapshot_time) != 0",
                "월요일 00:00만 보존"
            );
            case MONTHLY -> new CleanupConfig(
                "ranking_snapshot_monthly",
                "DAY(snapshot_time) != 1 OR HOUR(snapshot_time) != 0 OR MINUTE(snapshot_time) != 0",
                "매월 1일 00:00만 보존"
            );
        };
    }

    /**
     * 정리 설정을 담는 record
     */
    private record CleanupConfig(
        String tableName,
        String whereCondition,
        String description
    ) {}
}

