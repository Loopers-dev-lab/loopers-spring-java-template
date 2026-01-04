package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ProductRank 집계를 위한 Spring Batch Job Configuration.
 * <p>
 * 주간/월간 TOP 100 랭킹을 Materialized View에 저장합니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li><b>Step 1 (집계 로직 계산):</b> 모든 ProductMetrics를 읽어서 product_id별로 점수 집계</li>
 *   <li><b>Step 2 (랭킹 로직 실행):</b> 집계된 전체 데이터를 기반으로 TOP 100 선정 및 랭킹 번호 부여</li>
 *   <li><b>Chunk-Oriented Processing:</b> 대량 데이터를 메모리 효율적으로 처리</li>
 *   <li><b>Materialized View 저장:</b> 조회 성능 최적화를 위한 TOP 100 랭킹 저장</li>
 * </ul>
 * </p>
 * <p>
 * <b>Job 파라미터:</b>
 * <ul>
 *   <li>periodType: 기간 타입 (WEEKLY 또는 MONTHLY)</li>
 *   <li>targetDate: 기준 날짜 (yyyyMMdd 형식, 예: "20241215")</li>
 * </ul>
 * </p>
 * <p>
 * <b>실행 예시:</b>
 * <pre>
 * // 주간 집계
 * java -jar commerce-batch.jar \
 *   --spring.batch.job.names=productRankAggregationJob \
 *   periodType=WEEKLY targetDate=20241215
 *
 * // 월간 집계
 * java -jar commerce-batch.jar \
 *   --spring.batch.job.names=productRankAggregationJob \
 *   periodType=MONTHLY targetDate=20241215
 * </pre>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductRankJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductRankAggregationReader productRankAggregationReader;
    private final ProductRankAggregationProcessor productRankAggregationProcessor;
    private final ProductRankScoreAggregationWriter productRankScoreAggregationWriter;
    private final ProductRankCalculationReader productRankCalculationReader;
    private final ProductRankCalculationProcessor productRankCalculationProcessor;
    private final ProductRankCalculationWriter productRankCalculationWriter;

    /**
     * ProductRank 집계 Job을 생성합니다.
     * <p>
     * 2-Step 구조:
     * <ol>
     *   <li>Step 1: 집계 로직 계산 (점수 집계)</li>
     *   <li>Step 2: 랭킹 로직 실행 (TOP 100 선정 및 랭킹 번호 부여)</li>
     * </ol>
     * </p>
     *
     * @param scoreAggregationStep Step 1: 집계 로직 계산 Step
     * @param rankingCalculationStep Step 2: 랭킹 로직 실행 Step
     * @return ProductRank 집계 Job
     */
    @Bean
    public Job productRankAggregationJob(
        Step scoreAggregationStep,
        Step rankingCalculationStep
    ) {
        return new JobBuilder("productRankAggregationJob", jobRepository)
            .start(scoreAggregationStep)        // Step 1 먼저 실행
            .next(rankingCalculationStep)       // Step 1 완료 후 Step 2 실행
            .build();
    }

    /**
     * Step 1: 집계 로직 계산 Step을 생성합니다.
     * <p>
     * 모든 ProductMetrics를 읽어서 product_id별로 점수 집계하여 임시 테이블에 저장합니다.
     * </p>
     * <p>
     * Chunk-Oriented Processing을 사용하여:
     * <ol>
     *   <li>Reader: 특정 기간의 product_metrics를 페이징하여 읽기</li>
     *   <li>Processor: Pass-through (필터링 필요 시 추가 가능)</li>
     *   <li>Writer: product_id별로 점수 집계하여 ProductRankScore 테이블에 저장</li>
     * </ol>
     * </p>
     *
     * @param productRankReader ProductRank Reader (StepScope Bean)
     * @param productRankScoreWriter ProductRankScore Writer
     * @return 집계 로직 계산 Step
     */
    @Bean
    public Step scoreAggregationStep(
        ItemReader<ProductMetrics> productRankReader,
        ItemWriter<ProductMetrics> productRankScoreWriter
    ) {
        return new StepBuilder("scoreAggregationStep", jobRepository)
            .<ProductMetrics, ProductMetrics>chunk(100, transactionManager) // Chunk 크기: 100
            .reader(productRankReader)
            .processor(item -> item) // Pass-through
            .writer(productRankScoreWriter)
            .build();
    }

    /**
     * Step 2: 랭킹 로직 실행 Step을 생성합니다.
     * <p>
     * 집계된 전체 데이터를 기반으로 TOP 100 선정 및 랭킹 번호 부여하여 Materialized View에 저장합니다.
     * </p>
     * <p>
     * Chunk-Oriented Processing을 사용하여:
     * <ol>
     *   <li>Reader: ProductRankScore 테이블에서 모든 데이터를 점수 내림차순으로 읽기</li>
     *   <li>Processor: TOP 100 선정 및 랭킹 번호 부여</li>
     *   <li>Writer: ProductRank를 수집하고 저장</li>
     * </ol>
     * </p>
     *
     * @param productRankScoreReader ProductRankScore Reader
     * @param productRankCalculationProcessor ProductRank 계산 Processor
     * @param productRankCalculationWriter ProductRank 계산 Writer
     * @return 랭킹 로직 실행 Step
     */
    @Bean
    public Step rankingCalculationStep(
        ItemReader<ProductRankScore> productRankScoreReader,
        ItemProcessor<ProductRankScore, ProductRank> productRankCalculationProcessor,
        ItemWriter<ProductRank> productRankCalculationWriter
    ) {
        return new StepBuilder("rankingCalculationStep", jobRepository)
            .<ProductRankScore, ProductRank>chunk(100, transactionManager) // Chunk 크기: 100
            .reader(productRankScoreReader)
            .processor(productRankCalculationProcessor)
            .writer(productRankCalculationWriter)
            .build();
    }

    /**
     * ProductRank Reader를 생성합니다.
     * <p>
     * StepScope로 선언된 Bean이므로 Step 실행 시점에 Job 파라미터를 받아 생성됩니다.
     * </p>
     *
     * @param periodType 기간 타입 (Job 파라미터에서 주입)
     * @param targetDate 기준 날짜 (Job 파라미터에서 주입)
     * @return ProductRank Reader (StepScope로 선언되어 Step 실행 시 생성)
     */
    @Bean
    @StepScope
    public ItemReader<ProductMetrics> productRankReader(
        @Value("#{jobParameters['periodType']}") String periodType,
        @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        if (periodType == null || periodType.isEmpty()) {
            throw new IllegalArgumentException("periodType 파라미터는 필수입니다. (WEEKLY 또는 MONTHLY)");
        }

        LocalDate date = parseDate(targetDate);
        ProductRank.PeriodType period = ProductRank.PeriodType.valueOf(periodType.toUpperCase());

        // Processor에 기간 정보 설정
        productRankAggregationProcessor.setPeriod(period, date);

        if (period == ProductRank.PeriodType.WEEKLY) {
            return productRankAggregationReader.createWeeklyReader(date);
        } else {
            return productRankAggregationReader.createMonthlyReader(date);
        }
    }

    /**
     * Step 1용 ProductRankScore Writer를 주입받습니다.
     *
     * @return ProductRankScore Writer
     */
    @Bean
    public ItemWriter<ProductMetrics> productRankScoreWriter() {
        return productRankScoreAggregationWriter;
    }

    /**
     * Step 2용 ProductRankScore Reader를 주입받습니다.
     *
     * @return ProductRankScore Reader
     */
    @Bean
    public ItemReader<ProductRankScore> productRankScoreReader() {
        return productRankCalculationReader;
    }

    /**
     * Step 2용 ProductRank 계산 Processor를 주입받습니다.
     *
     * @return ProductRank 계산 Processor
     */
    @Bean
    public ItemProcessor<ProductRankScore, ProductRank> productRankCalculationProcessor() {
        return productRankCalculationProcessor;
    }

    /**
     * Step 2용 ProductRank 계산 Writer를 주입받습니다.
     *
     * @return ProductRank 계산 Writer
     */
    @Bean
    public ItemWriter<ProductRank> productRankCalculationWriter() {
        return productRankCalculationWriter;
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱합니다.
     *
     * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
     * @return 파싱된 날짜
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            log.warn("날짜 파라미터가 없어 오늘 날짜를 사용합니다.");
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 오늘 날짜를 사용합니다.", dateStr, e);
            return LocalDate.now();
        }
    }
}

