package com.loopers.batch.job.ranking;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.batch.job.ranking.processor.RankingProcessor;
import com.loopers.batch.job.ranking.reader.MonthlyMetricsReader;
import com.loopers.batch.job.ranking.writer.MonthlyRankWriter;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 월간 랭킹 배치 Job 설정
 * 
 * 실행 방법:
 * java -jar commerce-batch.jar --spring.batch.job.name=monthlyRankingJob --yearMonth=2024-12
 */
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = MonthlyRankingJobConfig.JOB_NAME)
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

    public static final String JOB_NAME = "monthlyRankingJob";
    private static final String STEP_NAME = "monthlyAggregationStep";
    private static final int CHUNK_SIZE = 100;  // TOP 100이므로 한 번에 처리

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final MonthlyMetricsReader monthlyMetricsReader;
    private final RankingProcessor rankingProcessor;
    private final MonthlyRankWriter monthlyRankWriter;

    /**
     * 월간 랭킹 집계 Job
     * 
     * @return 월간 랭킹 Job
     */
    @Bean(JOB_NAME)
    public Job monthlyRankingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlyAggregationStep())
                .listener(jobListener)
                .build();
    }

    /**
     * 월간 랭킹 집계 Step
     * - Reader: 월간 메트릭 데이터 집계 및 TOP 100 선별
     * - Processor: 추가 가공 (현재는 pass-through)
     * - Writer: mv_product_rank_monthly 테이블에 저장
     * 
     * @return 월간 집계 Step
     */
    @Bean(STEP_NAME)
    @JobScope
    public Step monthlyAggregationStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<RankingAggregation, RankingAggregation>chunk(CHUNK_SIZE, transactionManager)
                .reader(monthlyMetricsReader)
                .processor(rankingProcessor)
                .writer(monthlyRankWriter)
                .listener(stepMonitorListener)
                .build();
    }
}