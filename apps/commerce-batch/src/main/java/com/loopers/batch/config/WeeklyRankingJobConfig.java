package com.loopers.batch.config;

import com.loopers.batch.processor.RankingScoreProcessor;
import com.loopers.batch.reader.ProductMetricsAggregateReader;
import com.loopers.batch.writer.WeeklyRankingWriter;
import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.WeeklyProductRank;
import com.loopers.domain.rank.WeeklyRankRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 주간 랭킹 집계 배치 작업 설정 클래스.
 *
 * <p>이 작업은 상품 지표 데이터를 주간 단위로 집계하여
 * 상위 N개 랭킹을 Materialized View 테이블에 저장합니다.
 *
 * <p>작업 실행 예시:
 * <pre>
 * java -jar commerce-batch.jar \
 *   --job.name=weeklyRankingJob \
 *   yearWeek=2025-W01
 * </pre>
 *
 * <p>Chunk 지향 처리 흐름:
 * <ol>
 *   <li>Reader: product_metrics를 주간 단위로 집계</li>
 *   <li>Processor: 랭킹 점수 계산</li>
 *   <li>Writer: mv_product_rank_weekly에 저장</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyRankingJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;

    private final EntityManager entityManager;
    private final WeeklyRankRepository weeklyRankRepository;

    /**
     * 주간 랭킹 작업을 정의합니다.
     *
     * @param jobRepository Spring Batch 작업 저장소
     * @param weeklyRankingStep 실행할 Step
     * @return 설정된 Job 인스턴스
     */
    @Bean
    public Job weeklyRankingJob(
        JobRepository jobRepository,
        Step weeklyRankingStep
    ) {
        return new JobBuilder("weeklyRankingJob", jobRepository)
            .start(weeklyRankingStep)
            .build();
    }

    /**
     * Chunk 지향 처리를 사용하는 주간 랭킹 Step을 정의합니다.
     *
     * @param jobRepository Spring Batch 작업 저장소
     * @param transactionManager 트랜잭션 관리자
     * @param yearWeek ISO 형식의 대상 주차 (작업 파라미터에서 주입)
     * @return 설정된 Step 인스턴스
     */
    @Bean
    @JobScope
    public Step weeklyRankingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        log.info("Initializing weekly ranking step: yearWeek={}", yearWeek);

        return new StepBuilder("weeklyRankingStep", jobRepository)
            .<ProductRankingAggregation, WeeklyProductRank>chunk(CHUNK_SIZE, transactionManager)
            .reader(weeklyMetricsReader(yearWeek))
            .processor(weeklyRankingProcessor(yearWeek))
            .writer(weeklyRankingWriter())
            .build();
    }

    /**
     * 주간 지표 집계를 위한 ItemReader를 생성합니다.
     *
     * @param yearWeek 대상 주차
     * @return 설정된 ItemReader
     */
    @Bean
    @StepScope
    public ItemReader<ProductRankingAggregation> weeklyMetricsReader(
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        return new ProductMetricsAggregateReader(entityManager, yearWeek, "WEEKLY", TOP_N);
    }

    /**
     * 랭킹 점수 계산을 위한 ItemProcessor를 생성합니다.
     *
     * @param yearWeek 대상 주차
     * @return 설정된 ItemProcessor
     */
    @Bean
    @StepScope
    public ItemProcessor<ProductRankingAggregation, WeeklyProductRank> weeklyRankingProcessor(
        @Value("#{jobParameters['yearWeek']}") String yearWeek
    ) {
        RankingScoreProcessor processor = new RankingScoreProcessor("WEEKLY", yearWeek);
        return item -> (WeeklyProductRank) processor.process(item);
    }

    /**
     * 주간 랭킹 저장을 위한 ItemWriter를 생성합니다.
     *
     * @return 설정된 ItemWriter
     */
    @Bean
    @StepScope
    public ItemWriter<WeeklyProductRank> weeklyRankingWriter() {
        return new WeeklyRankingWriter(weeklyRankRepository);
    }
}
