package com.loopers.application.batch;

import com.loopers.application.batch.dto.ProductMetricsDto;
import com.loopers.application.batch.dto.RankedProductDto;
import com.loopers.application.batch.processor.RankingScoreProcessor;
import com.loopers.application.batch.writer.InMemoryRankingCollector;
import com.loopers.domain.ranking.PeriodUtils;
import com.loopers.domain.ranking.weekly.WeeklyRanking;
import com.loopers.domain.ranking.weekly.WeeklyRankingRepository;
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
 * 주간 랭킹 집계 배치 Job (Chunk-Oriented Processing)
 * - Reader: JdbcPagingItemReader로 ProductMetrics 조회
 * - Processor: 점수 계산
 * - Writer: 메모리에 수집
 * - Listener: Step 완료 후 정렬하여 TOP 100 저장
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeeklyRankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final WeeklyRankingRepository weeklyRankingRepository;

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
    public Job weeklyRankingJob() {
        return new JobBuilder("weeklyRankingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(weeklyRankingStep())
            .build();
    }

    @Bean
    public Step weeklyRankingStep() {
        InMemoryRankingCollector collector = new InMemoryRankingCollector();

        return new StepBuilder("weeklyRankingStep", jobRepository)
            .<ProductMetricsDto, RankedProductDto>chunk(CHUNK_SIZE, transactionManager)
            .reader(weeklyRankingReader())
            .processor(new RankingScoreProcessor(viewWeight, likeWeight, orderWeight))
            .writer(collector)
            .listener(weeklyRankingStepListener(collector))
            .build();
    }

    /**
     * Reader: ProductMetrics 테이블을 페이징으로 읽기
     */
    private JdbcPagingItemReader<ProductMetricsDto> weeklyRankingReader() {
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
        reader.setName("weeklyRankingReader");

        // IMPORTANT: Reader 초기화 필수
        try {
            reader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize weeklyRankingReader", e);
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
    private StepExecutionListener weeklyRankingStepListener(InMemoryRankingCollector collector) {
        return new StepExecutionListener() {

            @Override
            public void beforeStep(StepExecution stepExecution) {
                // Collector 초기화
                collector.clear();
                log.info("[WeeklyRanking] Step 시작 - Collector 초기화 완료");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                String targetDateParam = stepExecution.getJobParameters()
                    .getString("targetDate");

                if (targetDateParam == null) {
                    log.error("[WeeklyRanking] targetDate 파라미터가 없습니다");
                    return ExitStatus.FAILED;
                }

                LocalDate targetDate = LocalDate.parse(targetDateParam);
                LocalDate weekStartDate = PeriodUtils.getWeekStartDate(targetDate);
                LocalDate weekEndDate = PeriodUtils.getWeekEndDate(targetDate);

                log.info("[WeeklyRanking] Step 완료 - 주간: {} ~ {}", weekStartDate, weekEndDate);

                // 1. 기존 주간 랭킹 데이터 삭제
                weeklyRankingRepository.deleteByWeekStartDate(weekStartDate);
                log.info("[WeeklyRanking] 기존 주간 랭킹 데이터 삭제 완료: {}", weekStartDate);

                // 2. 수집된 데이터 가져오기
                List<RankedProductDto> collectedItems = collector.getCollectedItems();
                log.info("[WeeklyRanking] 총 {} 개 상품 메트릭 수집 완료", collectedItems.size());

                if (collectedItems.isEmpty()) {
                    log.warn("[WeeklyRanking] 집계할 데이터가 없습니다");
                    return ExitStatus.COMPLETED;
                }

                // 3. 정렬 (점수 내림차순)
                Collections.sort(collectedItems);

                // 4. TOP 100만 선택
                List<RankedProductDto> topRankings = collectedItems.stream()
                    .limit(TOP_N)
                    .toList();

                log.info("[WeeklyRanking] TOP {} 선택 완료", topRankings.size());

                // 5. 순위 설정 및 저장
                AtomicInteger rank = new AtomicInteger(1);
                List<WeeklyRanking> rankedList = topRankings.stream()
                    .map(dto -> new WeeklyRanking(
                        rank.getAndIncrement(),
                        dto.productId(),
                        dto.totalScore(),
                        weekStartDate,
                        weekEndDate
                    ))
                    .toList();

                weeklyRankingRepository.saveAll(rankedList);

                log.info("[WeeklyRanking] 주간 랭킹 집계 완료 - {} 건 저장", rankedList.size());

                return ExitStatus.COMPLETED;
            }
        };
    }
}
