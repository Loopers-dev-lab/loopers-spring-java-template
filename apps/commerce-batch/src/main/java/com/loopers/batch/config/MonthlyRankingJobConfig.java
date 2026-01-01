package com.loopers.batch.config;

import com.loopers.batch.processor.RankingScoreProcessor;
import com.loopers.batch.reader.ProductMetricsAggregateReader;
import com.loopers.batch.writer.MonthlyRankingWriter;
import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
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
 * 월간 랭킹 집계 배치 작업 설정 클래스.
 *
 * <p>이 작업은 상품 지표 데이터를 월간 단위로 집계하여
 * 상위 N개 랭킹을 Materialized View 테이블에 저장합니다.
 *
 * <p>작업 실행 예시:
 * <pre>
 * java -jar commerce-batch.jar \
 *   --job.name=monthlyRankingJob \
 *   yearMonth=2025-01
 * </pre>
 *
 * <p>Chunk 지향 처리 흐름:
 * <ol>
 *   <li>Reader: product_metrics를 월간 단위로 집계</li>
 *   <li>Processor: 랭킹 점수 계산</li>
 *   <li>Writer: mv_product_rank_monthly에 저장</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;

    private final EntityManager entityManager;
    private final MonthlyRankRepository monthlyRankRepository;

    /**
     * 월간 랭킹 작업을 정의합니다.
     *
     * @param jobRepository Spring Batch 작업 저장소
     * @param monthlyRankingStep 실행할 Step
     * @return 설정된 Job 인스턴스
     */
    @Bean
    public Job monthlyRankingJob(
        JobRepository jobRepository,
        Step monthlyRankingStep
    ) {
        return new JobBuilder("monthlyRankingJob", jobRepository)
            .start(monthlyRankingStep)
            .build();
    }

    /**
     * Chunk 지향 처리를 사용하는 월간 랭킹 Step을 정의합니다.
     *
     * @param jobRepository Spring Batch 작업 저장소
     * @param transactionManager 트랜잭션 관리자
     * @param yearMonth 대상 월 (작업 파라미터에서 주입)
     * @return 설정된 Step 인스턴스
     */
    @Bean
    @JobScope
    public Step monthlyRankingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        log.info("Initializing monthly ranking step: yearMonth={}", yearMonth);

        return new StepBuilder("monthlyRankingStep", jobRepository)
            .<ProductRankingAggregation, MonthlyProductRank>chunk(CHUNK_SIZE, transactionManager)
            .reader(monthlyMetricsReader(yearMonth))
            .processor(monthlyRankingProcessor(yearMonth))
            .writer(monthlyRankingWriter())
            .build();
    }

    /**
     * 월간 지표 집계를 위한 ItemReader를 생성합니다.
     *
     * @param yearMonth 대상 월
     * @return 설정된 ItemReader
     */
    @Bean
    @StepScope
    public ItemReader<ProductRankingAggregation> monthlyMetricsReader(
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        return new ProductMetricsAggregateReader(entityManager, yearMonth, "MONTHLY", TOP_N);
    }

    /**
     * 랭킹 점수 계산을 위한 ItemProcessor를 생성합니다.
     *
     * @param yearMonth 대상 월
     * @return 설정된 ItemProcessor
     */
    @Bean
    @StepScope
    public ItemProcessor<ProductRankingAggregation, MonthlyProductRank> monthlyRankingProcessor(
        @Value("#{jobParameters['yearMonth']}") String yearMonth
    ) {
        RankingScoreProcessor processor = new RankingScoreProcessor("MONTHLY", yearMonth);
        return item -> (MonthlyProductRank) processor.process(item);
    }

    /**
     * 월간 랭킹 저장을 위한 ItemWriter를 생성합니다.
     *
     * @return 설정된 ItemWriter
     */
    @Bean
    @StepScope
    public ItemWriter<MonthlyProductRank> monthlyRankingWriter() {
        return new MonthlyRankingWriter(monthlyRankRepository);
    }
}
