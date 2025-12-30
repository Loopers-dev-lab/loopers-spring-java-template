package com.loopers.batch.job;

import com.loopers.domain.ranking.ProductMetricsWeekly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.item.database.Order;
import com.loopers.domain.metrics.ProductMetricsRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyRankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    @Qualifier("dataSource")
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final ProductMetricsRepository productMetricsRepository;

    @Bean
    public Job weeklyRankingJob() {
        return new JobBuilder("weeklyRankingJob", jobRepository)
                .start(weeklyRankingStep())
                .build();
    }

    @Bean
    public Step weeklyRankingStep() {
        return new StepBuilder("weeklyRankingStep", jobRepository)
                .<WeeklyMetricsDto, ProductMetricsWeekly>chunk(1000, transactionManager)
                .reader(weeklyMetricsReader())
                .processor(weeklyMetricsProcessor(null))
                .writer(weeklyMetricsWriter())
                .build();
    }

    @Bean
    public ItemReader<WeeklyMetricsDto> weeklyMetricsReader() {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("pm.product_id, COALESCE(SUM(pm.like_count), 0) as like_count, COALESCE(SUM(pm.sales_revenue), 0) as order_count, COALESCE(SUM(pm.view_count), 0) as view_count");
        queryProvider.setFromClause("product_metrics pm");
        queryProvider.setWhereClause("pm.bucket_time_key >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 7 DAY), '%Y%m%d%H')");
        queryProvider.setGroupClause("pm.product_id");
        queryProvider.setSortKeys(Map.of("pm.product_id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<WeeklyMetricsDto>()
                .name("weeklyMetricsReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .pageSize(1000)
                .rowMapper(new BeanPropertyRowMapper<>(WeeklyMetricsDto.class))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<WeeklyMetricsDto, ProductMetricsWeekly> weeklyMetricsProcessor(
            @Value("#{jobParameters['period']}") String period) {
        return dto -> {
            String yearMonthWeek = period != null ? period : getCurrentYearMonthWeek();
            log.debug("Processing weekly metrics for product: {}, week: {}", dto.getProductId(), yearMonthWeek);
            
            return new ProductMetricsWeekly(
                dto.getProductId(),
                dto.getLikeCount(),
                dto.getOrderCount(),
                dto.getViewCount(),
                yearMonthWeek
            );
        };
    }

    @Bean
    public ItemWriter<ProductMetricsWeekly> weeklyMetricsWriter() {
        return new JpaItemWriterBuilder<ProductMetricsWeekly>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    private String getCurrentYearMonthWeek() {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekOfYear = now.get(weekFields.weekOfYear());
        return now.format(DateTimeFormatter.ofPattern("yyyy")) + String.format("%02d", weekOfYear);
    }

    public static class WeeklyMetricsDto {
        private Long productId;
        private Integer likeCount;
        private Integer orderCount;
        private Integer viewCount;

        // getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getLikeCount() { return likeCount; }
        public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
        public Integer getViewCount() { return viewCount; }
        public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    }
}