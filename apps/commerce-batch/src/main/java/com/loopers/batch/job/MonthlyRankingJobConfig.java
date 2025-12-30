package com.loopers.batch.job;

import com.loopers.domain.ranking.ProductMetricsMonthly;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    @Qualifier("mySqlMainDataSource")
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job monthlyRankingJob() {
        return new JobBuilder("monthlyRankingJob", jobRepository)
                .start(monthlyRankingStep())
                .build();
    }

    @Bean
    public Step monthlyRankingStep() {
        return new StepBuilder("monthlyRankingStep", jobRepository)
                .<MonthlyMetricsDto, ProductMetricsMonthly>chunk(1000, transactionManager)
                .reader(monthlyMetricsReader())
                .processor(monthlyMetricsProcessor(null))
                .writer(monthlyMetricsWriter())
                .build();
    }

    @Bean
    public ItemReader<MonthlyMetricsDto> monthlyMetricsReader() {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("pm.product_id, COALESCE(SUM(pm.like_count), 0) as like_count, COALESCE(SUM(pm.sales_revenue), 0) as order_count, COALESCE(SUM(pm.view_count), 0) as view_count");
        queryProvider.setFromClause("product_metrics pm");
        queryProvider.setWhereClause("pm.bucket_time_key >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y%m%d%H')");
        queryProvider.setGroupClause("pm.product_id");
        queryProvider.setSortKeys(Map.of("pm.product_id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<MonthlyMetricsDto>()
                .name("monthlyMetricsReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .pageSize(1000)
                .rowMapper(new BeanPropertyRowMapper<>(MonthlyMetricsDto.class))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<MonthlyMetricsDto, ProductMetricsMonthly> monthlyMetricsProcessor(
            @Value("#{jobParameters['period']}") String period) {
        return dto -> {
            String yearMonth = period != null ? period : getCurrentYearMonth();
            log.debug("Processing monthly metrics for product: {}, month: {}", dto.getProductId(), yearMonth);
            
            return new ProductMetricsMonthly(
                dto.getProductId(),
                dto.getLikeCount(),
                dto.getOrderCount(),
                dto.getViewCount(),
                yearMonth
            );
        };
    }

    @Bean
    public ItemWriter<ProductMetricsMonthly> monthlyMetricsWriter() {
        return new JpaItemWriterBuilder<ProductMetricsMonthly>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    private String getCurrentYearMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    public static class MonthlyMetricsDto {
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
