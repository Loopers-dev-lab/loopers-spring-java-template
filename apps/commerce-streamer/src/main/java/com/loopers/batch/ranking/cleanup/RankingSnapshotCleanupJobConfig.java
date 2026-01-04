package com.loopers.batch.ranking.cleanup;

import com.loopers.batch.ranking.RankingType;
import com.loopers.config.batch.RankingJobExecutionListener;
import com.loopers.infrastructure.ranking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 스냅샷 정리 Job Configuration
 * 
 * 매일 새벽 2시 실행 (권장)
 * 불필요한 중간 스냅샷 삭제 (정시/일시작/주시작/월시작만 보존)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingSnapshotCleanupJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RankingSnapshotHourlyJpaRepository hourlySnapshotRepository;
    private final RankingSnapshotDailyJpaRepository dailySnapshotRepository;
    private final RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository;
    private final RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository;
    private final RankingJobExecutionListener rankingJobExecutionListener;

    /**
     * RankingSnapshotCleanupJob 정의
     */
    @Bean
    public Job rankingSnapshotCleanupJob() {
        // 첫 번째 Step으로 시작
        Step firstStep = createSnapshotCleanupStep(RankingType.HOURLY);
        var jobBuilder = new JobBuilder("rankingSnapshotCleanupJob", jobRepository)
            .start(firstStep);
        
        // 나머지 Step들을 순차적으로 추가
        for (RankingType type : new RankingType[]{RankingType.DAILY, RankingType.WEEKLY, RankingType.MONTHLY}) {
            Step step = createSnapshotCleanupStep(type);
            jobBuilder = jobBuilder.next(step);
        }
        
        return jobBuilder
            .listener(rankingJobExecutionListener)
            .build();
    }

    /**
     * 랭킹 타입별 스냅샷 정리 Step 생성
     */
    private Step createSnapshotCleanupStep(RankingType rankingType) {
        String stepName = rankingType.name().toLowerCase() + "SnapshotCleanupStep";
        return new StepBuilder(stepName, jobRepository)
            .tasklet(createSnapshotCleanupTasklet(rankingType), transactionManager)
            .build();
    }

    /**
     * 랭킹 타입별 스냅샷 정리 Tasklet 생성
     */
    private Tasklet createSnapshotCleanupTasklet(RankingType rankingType) {
        return new SnapshotCleanupTasklet(
            rankingType,
            hourlySnapshotRepository,
            dailySnapshotRepository,
            weeklySnapshotRepository,
            monthlySnapshotRepository
        );
    }
}

