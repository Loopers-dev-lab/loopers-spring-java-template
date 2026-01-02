package com.loopers.infrastructure.batch;

import com.loopers.infrastructure.ranking.ProductRankMonthly;
import com.loopers.infrastructure.ranking.ProductRankMonthlyRepository;
import com.loopers.infrastructure.ranking.RankingEntry;
import com.loopers.infrastructure.ranking.RankingRedisService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MonthlyRankingJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final int TOP_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final ProductRankMonthlyRepository productRankMonthlyRepository;
    private final RankingRedisService rankingRedisService;

    public MonthlyRankingJobConfig(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   DataSource dataSource,
                                   ProductRankMonthlyRepository productRankMonthlyRepository,
                                   RankingRedisService rankingRedisService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.productRankMonthlyRepository = productRankMonthlyRepository;
        this.rankingRedisService = rankingRedisService;
    }

    @Bean
    public Job monthlyRankingJob() {
        return new JobBuilder("monthlyRankingJob", jobRepository)
                .start(monthlyRankingStep())
                .build();
    }

    @Bean
    public Step monthlyRankingStep() {
        return new StepBuilder("monthlyRankingStep", jobRepository)
                .<ProductMetricsAggregation, ProductRankMonthly>chunk(CHUNK_SIZE, transactionManager)
                .reader(monthlyMetricsReader(null))
                .processor(monthlyRankingProcessor(null))
                .writer(monthlyRankingWriter())
                .listener(monthlyRankingStepListener(null))
                .build();
    }

    @Bean
    @StepScope
    public StepExecutionListener monthlyRankingStepListener(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
                YearMonth yearMonth = YearMonth.from(targetDate);
                LocalDate monthStart = yearMonth.atDay(1);
                productRankMonthlyRepository.deleteByPeriodStart(monthStart);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
                YearMonth yearMonth = YearMonth.from(targetDate);
                LocalDate monthStart = yearMonth.atDay(1);

                List<ProductRankMonthly> monthlyRanks = productRankMonthlyRepository
                        .findByPeriodStartOrderByRankingAsc(monthStart);

                if (!monthlyRanks.isEmpty()) {
                    List<RankingEntry> entries = monthlyRanks.stream()
                            .map(r -> new RankingEntry(r.getProductId(), r.getScore().doubleValue()))
                            .toList();
                    rankingRedisService.cacheMonthlyRanking(monthStart, entries);
                }

                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<ProductMetricsAggregation> monthlyMetricsReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
        YearMonth yearMonth = YearMonth.from(targetDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startDate", monthStart);
        parameterValues.put("endDate", monthEnd);

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("score", Order.DESCENDING);

        RowMapper<ProductMetricsAggregation> rowMapper = (rs, rowNum) -> new ProductMetricsAggregation(
                rs.getLong("product_id"),
                rs.getLong("viewCount"),
                rs.getLong("likeCount"),
                rs.getLong("orderCount"),
                rs.getLong("totalQuantity"),
                rs.getLong("score")
        );

        return new JdbcPagingItemReaderBuilder<ProductMetricsAggregation>()
                .name("monthlyMetricsReader")
                .dataSource(dataSource)
                .selectClause("SELECT product_id, " +
                        "SUM(view_count) as viewCount, " +
                        "SUM(like_count) as likeCount, " +
                        "SUM(order_count) as orderCount, " +
                        "SUM(total_quantity) as totalQuantity, " +
                        "(SUM(view_count) + SUM(like_count) * 2 + SUM(order_count) * 6) as score")
                .fromClause("FROM product_metrics")
                .whereClause("WHERE date BETWEEN :startDate AND :endDate")
                .groupClause("GROUP BY product_id")
                .sortKeys(sortKeys)
                .parameterValues(parameterValues)
                .rowMapper(rowMapper)
                .pageSize(CHUNK_SIZE)
                .maxItemCount(TOP_LIMIT)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<ProductMetricsAggregation, ProductRankMonthly> monthlyRankingProcessor(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
        YearMonth yearMonth = YearMonth.from(targetDate);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        AtomicInteger rankCounter = new AtomicInteger(0);

        return aggregation -> ProductRankMonthly.create(
                aggregation.getProductId(),
                monthStart,
                monthEnd,
                rankCounter.incrementAndGet(),
                aggregation.getScore(),
                aggregation.getViewCount(),
                aggregation.getLikeCount(),
                aggregation.getOrderCount(),
                aggregation.getTotalQuantity()
        );
    }

    @Bean
    public ItemWriter<ProductRankMonthly> monthlyRankingWriter() {
        return items -> {
            if (!items.isEmpty()) {
                productRankMonthlyRepository.saveAll(items);
            }
        };
    }
}
