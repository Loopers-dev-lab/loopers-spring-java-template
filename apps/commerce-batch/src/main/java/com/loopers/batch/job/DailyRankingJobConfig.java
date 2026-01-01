package com.loopers.batch.job;

import com.loopers.domain.ranking.ProductMetricsDaily;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyRankingJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final DataSource dataSource;
  private final EntityManagerFactory entityManagerFactory;

  @Bean
  public Job dailyRankingDataProcessingJob() {
    return new JobBuilder("dailyRankingDataProcessingJob", jobRepository)
        .start(clearDailyWorkingTablesStep())
        .next(dailyRankingStep())
        .next(dailyDataValidationStep())
        .next(dailyTableSwapStep())
        .build();
  }

  // 1단계: Working 테이블 데이터 삭제
  @Bean
  public Step clearDailyWorkingTablesStep() {
    return new StepBuilder("clearDailyWorkingTablesStep", jobRepository)
        .tasklet(clearDailyWorkingTablesTasklet(), transactionManager)
        .build();
  }

  // 2단계: 일간 랭킹 데이터 적재 (특정 일자)
  @Bean
  public Step dailyRankingStep() {
    return new StepBuilder("dailyRankingStep", jobRepository)
        .<DailyMetricsDto, ProductMetricsDaily>chunk(1000, transactionManager)
        .reader(dailyMetricsReader(null))
        .processor(dailyMetricsProcessor(null))
        .writer(dailyMetricsWriter())
        .build();
  }

  // 3단계: 데이터 검증
  @Bean
  public Step dailyDataValidationStep() {
    return new StepBuilder("dailyDataValidationStep", jobRepository)
        .tasklet(dailyDataValidationTasklet(), transactionManager)
        .build();
  }

  // 4단계: 테이블 교체
  @Bean
  public Step dailyTableSwapStep() {
    return new StepBuilder("dailyTableSwapStep", jobRepository)
        .tasklet(dailyTableSwapTasklet(), transactionManager)
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<DailyMetricsDto> dailyMetricsReader(@Value("#{jobParameters['date']}") String date) {
    MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
    queryProvider.setSelectClause("pm.product_id, COALESCE(SUM(pm.like_count), 0) as like_count, COALESCE(SUM(pm.sales_revenue), 0) as order_count, COALESCE(SUM(pm.view_count), 0) as view_count");
    queryProvider.setFromClause("product_metrics pm");

    // 특정 일의 데이터만 가져오기 (00:00부터 23:59까지)
    String currentDate = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String startKey = currentDate + "0000"; // 해당 일 00:00
    String endKey = currentDate + "2359";   // 해당 일 23:59

    queryProvider.setWhereClause("pm.bucket_time_key >= '" + startKey + "' AND pm.bucket_time_key <= '" + endKey + "'");
    queryProvider.setGroupClause("pm.product_id");
    queryProvider.setSortKeys(Map.of("pm.product_id", Order.ASCENDING));

    return new JdbcPagingItemReaderBuilder<DailyMetricsDto>()
        .name("dailyMetricsReader")
        .dataSource(dataSource)
        .queryProvider(queryProvider)
        .pageSize(1000)
        .rowMapper(new BeanPropertyRowMapper<>(DailyMetricsDto.class))
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<DailyMetricsDto, ProductMetricsDaily> dailyMetricsProcessor(
      @Value("#{jobParameters['date']}") String date) {
    return dto -> {
      String currentDate = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      log.debug("Processing daily metrics for product: {}, date: {}", dto.getProductId(), currentDate);

      return new ProductMetricsDaily(
          dto.getProductId(),
          dto.getLikeCount(),
          dto.getOrderCount(),
          dto.getViewCount(),
          currentDate
      );
    };
  }

  @Bean
  public ItemWriter<ProductMetricsDaily> dailyMetricsWriter() {
    return new JpaItemWriterBuilder<ProductMetricsDaily>()
        .entityManagerFactory(entityManagerFactory)
        .build();
  }

  public static class DailyMetricsDto {
    private Long productId;
    private Integer likeCount;
    private Integer orderCount;
    private Integer viewCount;

    // getters and setters
    public Long getProductId() {
      return productId;
    }

    public void setProductId(Long productId) {
      this.productId = productId;
    }

    public Integer getLikeCount() {
      return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
      this.likeCount = likeCount;
    }

    public Integer getOrderCount() {
      return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
      this.orderCount = orderCount;
    }

    public Integer getViewCount() {
      return viewCount;
    }

    public void setViewCount(Integer viewCount) {
      this.viewCount = viewCount;
    }
  }

  // Tasklet 구현들
  @Bean
  public Tasklet clearDailyWorkingTablesTasklet() {
    return (contribution, chunkContext) -> {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      log.info("Clearing daily working tables...");

      jdbcTemplate.execute("TRUNCATE TABLE product_metrics_daily_working");
      log.info("Successfully cleared product_metrics_daily_working table");

      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Tasklet dailyDataValidationTasklet() {
    return (contribution, chunkContext) -> {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      log.info("Starting daily data validation...");

      // 1. 데이터 건수 확인
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM product_metrics_daily_working", Integer.class);
      log.info("Daily working table record count: {}", count);

      if (count == 0) {
        throw new RuntimeException("No data found in daily working table");
      }

      // 2. 필수 필드 NULL 체크
      Integer nullCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM product_metrics_daily_working WHERE product_id IS NULL OR period_yyyymmdd IS NULL",
          Integer.class);

      if (nullCount > 0) {
        throw new RuntimeException("Found " + nullCount + " records with null required fields");
      }

      log.info("Daily data validation completed successfully");
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Tasklet dailyTableSwapTasklet() {
    return (contribution, chunkContext) -> {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      log.info("Starting daily table swap...");

      try {
        // 1. 이전 백업 삭제
        jdbcTemplate.execute("DROP TABLE IF EXISTS product_metrics_daily_backup");

        // 2. 원자적 테이블 교체
        jdbcTemplate.execute("""
                RENAME TABLE 
                    product_metrics_daily TO product_metrics_daily_backup,
                    product_metrics_daily_working TO product_metrics_daily
            """);

        log.info("Daily table swap completed successfully");
        return RepeatStatus.FINISHED;

      } catch (Exception e) {
        log.error("Daily table swap failed, rolling back...", e);
        throw e;
      }
    };
  }
}
