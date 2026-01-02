package com.loopers.batch.ranking.recovery;

import com.loopers.batch.ranking.RankingType;
import com.loopers.batch.ranking.step1.Aggregate5MinProcessor;
import com.loopers.batch.ranking.step2.RankingUpdateService;
import com.loopers.config.batch.RankingJobExecutionListener;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import com.loopers.infrastructure.ranking.RankingEventLogJpaRepository;
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
 * Full Re-sync Job Configuration
 * 
 * 일일 실행 (새벽 2시 권장)
 * 전체 윈도우 기간의 데이터를 재집계하여 데이터 정합성 보장
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingFullResyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RankingEventLogJpaRepository rankingEventLogJpaRepository;
    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;
    private final RankingUpdateService rankingUpdateService;
    private final RankingJobExecutionListener rankingJobExecutionListener;
    private final Aggregate5MinProcessor aggregate5MinProcessor;

    /**
     * RankingFullResyncJob 정의
     */
    @Bean
    public Job rankingFullResyncJob() {
        // 첫 번째 Step으로 시작
        Step firstStep = createFullResyncStep(RankingType.HOURLY);
        var jobBuilder = new JobBuilder("rankingFullResyncJob", jobRepository)
            .start(firstStep);
        
        // 나머지 Step들을 순차적으로 추가
        for (RankingType type : new RankingType[]{RankingType.DAILY, RankingType.WEEKLY, RankingType.MONTHLY}) {
            Step step = createFullResyncStep(type);
            jobBuilder = jobBuilder.next(step);
        }
        
        return jobBuilder
            .listener(rankingJobExecutionListener)
            .build();
    }

    /**
     * 랭킹 타입별 Full Re-sync Step 생성
     */
    private Step createFullResyncStep(RankingType rankingType) {
        String stepName = rankingType.name().toLowerCase() + "FullResyncStep";
        return new StepBuilder(stepName, jobRepository)
            .tasklet(createFullResyncTasklet(rankingType), transactionManager)
            .build();
    }

    /**
     * 랭킹 타입별 Full Re-sync Tasklet 생성
     */
    private Tasklet createFullResyncTasklet(RankingType rankingType) {
        return new FullResyncTasklet(
            rankingType,
            rankingEventLogJpaRepository,
            productScore5MinJpaRepository,
            rankingUpdateService,
            aggregate5MinProcessor
        );
    }
}

