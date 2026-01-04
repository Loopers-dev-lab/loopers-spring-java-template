package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
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

/**
 * ProductMetrics 집계를 위한 Spring Batch Job Configuration.
 * <p>
 * Chunk-Oriented Processing 방식을 사용하여 대량의 product_metrics 데이터를 처리합니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li><b>Chunk-Oriented Processing:</b> 대량 데이터를 메모리 효율적으로 처리</li>
 *   <li><b>Job 파라미터 기반 실행:</b> 날짜를 파라미터로 받아 특정 날짜의 데이터만 처리</li>
 *   <li><b>확장성:</b> 향후 주간/월간 집계를 위한 구조 준비</li>
 *   <li><b>재시작 가능:</b> 실패 시 이전 Chunk부터 재시작 가능</li>
 * </ul>
 * </p>
 * <p>
 * <b>Chunk 크기 선택 근거:</b>
 * <ul>
 *   <li>100개: 메모리 사용량과 성능의 균형</li>
 *   <li>너무 작으면: 트랜잭션 오버헤드 증가</li>
 *   <li>너무 크면: 메모리 사용량 증가 및 롤백 범위 확대</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductMetricsJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductMetricsItemReader productMetricsItemReader;
    private final ProductMetricsItemProcessor productMetricsItemProcessor;
    private final ProductMetricsItemWriter productMetricsItemWriter;

    /**
     * ProductMetrics 집계 Job을 생성합니다.
     * <p>
     * Job 파라미터:
     * <ul>
     *   <li>targetDate: 처리할 날짜 (yyyyMMdd 형식, 예: "20241215")</li>
     * </ul>
     * </p>
     * <p>
     * 실행 예시:
     * <pre>
     * java -jar commerce-batch.jar --spring.batch.job.names=productMetricsAggregationJob targetDate=20241215
     * </pre>
     * </p>
     *
     * @return ProductMetrics 집계 Job
     */
    @Bean
    public Job productMetricsAggregationJob(Step productMetricsAggregationStep) {
        return new JobBuilder("productMetricsAggregationJob", jobRepository)
            .start(productMetricsAggregationStep)
            .build();
    }

    /**
     * ProductMetrics 집계 Step을 생성합니다.
     * <p>
     * Chunk-Oriented Processing을 사용하여:
     * <ol>
     *   <li>Reader: 특정 날짜의 product_metrics를 페이징하여 읽기</li>
     *   <li>Processor: 데이터 변환/필터링 (현재는 pass-through)</li>
     *   <li>Writer: 집계 결과 처리 (현재는 로깅, 향후 MV 저장)</li>
     * </ol>
     * </p>
     *
     * @param productMetricsReader ProductMetrics Reader (StepScope Bean)
     * @param productMetricsProcessor ProductMetrics Processor
     * @param productMetricsWriter ProductMetrics Writer
     * @return ProductMetrics 집계 Step
     */
    @Bean
    public Step productMetricsAggregationStep(
        ItemReader<ProductMetrics> productMetricsReader,
        ItemProcessor<ProductMetrics, ProductMetrics> productMetricsProcessor,
        ItemWriter<ProductMetrics> productMetricsWriter
    ) {
        return new StepBuilder("productMetricsAggregationStep", jobRepository)
            .<ProductMetrics, ProductMetrics>chunk(100, transactionManager) // Chunk 크기: 100
            .reader(productMetricsReader) // StepScope Bean은 Step 실행 시점에 자동 주입됨
            .processor(productMetricsProcessor)
            .writer(productMetricsWriter)
            .build();
    }

    /**
     * ProductMetrics Reader를 생성합니다.
     * <p>
     * StepScope로 선언된 Bean이므로 Step 실행 시점에 Job 파라미터를 받아 생성됩니다.
     * </p>
     *
     * @param targetDate 조회할 날짜 (Job 파라미터에서 주입)
     * @return ProductMetrics Reader (StepScope로 선언되어 Step 실행 시 생성)
     */
    @Bean
    @StepScope
    public ItemReader<ProductMetrics> productMetricsReader(
        @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        return productMetricsItemReader.createReader(targetDate);
    }

    /**
     * ProductMetrics Processor를 주입받습니다.
     *
     * @return ProductMetrics Processor
     */
    @Bean
    public ItemProcessor<ProductMetrics, ProductMetrics> productMetricsProcessor() {
        return productMetricsItemProcessor;
    }

    /**
     * ProductMetrics Writer를 주입받습니다.
     *
     * @return ProductMetrics Writer
     */
    @Bean
    public ItemWriter<ProductMetrics> productMetricsWriter() {
        return productMetricsItemWriter;
    }
}

