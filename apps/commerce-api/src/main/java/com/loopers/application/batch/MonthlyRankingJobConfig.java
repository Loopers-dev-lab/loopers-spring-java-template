package com.loopers.application.batch;

import com.loopers.application.batch.dto.ProductMetricsDto;
import com.loopers.application.batch.dto.RankedProductDto;
import com.loopers.application.batch.processor.RankingScoreProcessor;
import com.loopers.application.batch.writer.InMemoryRankingCollector;
import com.loopers.domain.ranking.PeriodUtils;
import com.loopers.domain.ranking.monthly.MonthlyRanking;
import com.loopers.domain.ranking.monthly.MonthlyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 월간 랭킹 집계 배치 Job (Chunk-Oriented Processing)
 * - Reader: JdbcPagingItemReader로 ProductMetrics 조회
 * - Processor: 점수 계산
 * - Writer: 메모리에 수집
 * - Listener: Step 완료 후 정렬하여 TOP 100 저장
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MonthlyRankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final MonthlyRankingRepository monthlyRankingRepository;

    private static final int CHUNK_SIZE = 100;
    private static final int PAGE_SIZE = 100;
    private static final int TOP_N = 100;

    @Value("${ranking.weight.view:0.1}")
    private double viewWeight;

    @Value("${ranking.weight.like:0.2}")
    private double likeWeight;

    @Value("${ranking.weight.order:0.6}")
    private double orderWeight;

    @Bean
    public Job monthlyRankingJob() {
        return new JobBuilder("monthlyRankingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(monthlyRankingStep())
            .build();
    }

    @Bean
    public Step monthlyRankingStep() {
        InMemoryRankingCollector collector = new InMemoryRankingCollector();

        return new StepBuilder("monthlyRankingStep", jobRepository)
            .<ProductMetricsDto, RankedProductDto>chunk(CHUNK_SIZE, transactionManager)
            .reader(monthlyRankingReader())
            .processor(new RankingScoreProcessor(viewWeight, likeWeight, orderWeight))
            .writer(collector)
            .listener(monthlyRankingStepListener(collector))
            .build();
    }

    /**
     * Reader: ProductMetrics 테이블을 페이징으로 읽기
     */
    private JdbcPagingItemReader<ProductMetricsDto> monthlyRankingReader() {
        JdbcPagingItemReader<ProductMetricsDto> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(PAGE_SIZE);
        reader.setRowMapper(productMetricsRowMapper());

        // MySQL PagingQueryProvider
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT product_id, like_count, view_count, sales_count, sales_amount");
        queryProvider.setFromClause("FROM product_metrics");
        queryProvider.setSortKeys(Map.of("product_id", Order.ASCENDING)); // 정렬 기준 (페이징을 위해 필요)

        reader.setQueryProvider(queryProvider);
        reader.setName("monthlyRankingReader");

        // IMPORTANT: Reader 초기화 필수
        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize monthlyRankingReader", e);
        }

        return reader;
    }

    /**
     * RowMapper: ResultSet을 ProductMetricsDto로 변환
     */
    private RowMapper<ProductMetricsDto> productMetricsRowMapper() {
        return (rs, rowNum) -> new ProductMetricsDto(
            rs.getLong("product_id"),
            rs.getLong("like_count"),
            rs.getLong("view_count"),
            rs.getLong("sales_count"),
            rs.getLong("sales_amount")
        );
    }

    /**
     * StepExecutionListener: Step 완료 후 정렬 및 TOP 100 저장
     */
    private StepExecutionListener monthlyRankingStepListener(InMemoryRankingCollector collector) {
        return new StepExecutionListener() {

            @Override
            public void beforeStep(StepExecution stepExecution) {
                // Collector 초기화
                collector.clear();
                log.info("[MonthlyRanking] Step 시작 - Collector 초기화 완료");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                String targetDateParam = stepExecution.getJobParameters()
                    .getString("targetDate");

                if (targetDateParam == null) {
                    log.error("[MonthlyRanking] targetDate 파라미터가 없습니다");
                    return ExitStatus.FAILED;
                }

                LocalDate targetDate = LocalDate.parse(targetDateParam);
                PeriodUtils.MonthRange monthRange = PeriodUtils.MonthRange.from(targetDate);

                log.info("[MonthlyRanking] Step 완료 - 월간: {} ({} ~ {})",
                    monthRange.key(), monthRange.start(), monthRange.end());

                // 1. 기존 월간 랭킹 데이터 삭제
                monthlyRankingRepository.deleteByMonthYear(monthRange.key());
                log.info("[MonthlyRanking] 기존 월간 랭킹 데이터 삭제 완료: {}", monthRange.key());

                // 2. 수집된 데이터 가져오기
                List<RankedProductDto> collectedItems = collector.getCollectedItems();
                log.info("[MonthlyRanking] 총 {} 개 상품 메트릭 수집 완료", collectedItems.size());

                if (collectedItems.isEmpty()) {
                    log.warn("[MonthlyRanking] 집계할 데이터가 없습니다");
                    return ExitStatus.COMPLETED;
                }

                // 3. 정렬 (점수 내림차순)
                Collections.sort(collectedItems);

                // 4. TOP 100만 선택
                List<RankedProductDto> topRankings = collectedItems.stream()
                    .limit(TOP_N)
                    .toList();

                log.info("[MonthlyRanking] TOP {} 선택 완료", topRankings.size());

                // 5. 순위 설정 및 저장
                AtomicInteger rank = new AtomicInteger(1);
                List<MonthlyRanking> rankedList = topRankings.stream()
                    .map(dto -> new MonthlyRanking(
                        rank.getAndIncrement(),
                        dto.productId(),
                        dto.totalScore(),
                        monthRange.key(),
                        monthRange.start(),
                        monthRange.end()
                    ))
                    .toList();

                monthlyRankingRepository.saveAll(rankedList);

                log.info("[MonthlyRanking] 월간 랭킹 집계 완료 - {} 건 저장", rankedList.size());

                return ExitStatus.COMPLETED;
            }
        };
    }
}
