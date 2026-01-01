package com.loopers.batch.job;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final DataSource dataSource;
  private final EntityManagerFactory entityManagerFactory;
  private final RedisTemplate<String, String> redisTemplate;

  @Bean
  public Job monthlyRankingMVUpdateJob() {
    return new JobBuilder("monthlyRankingMVUpdateJob", jobRepository)
        .start(monthlyTop100MVUpdateStep())
        .build();
  }

  private String getCurrentYearMonth() {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
  }

  @Bean
  @StepScope
  public Tasklet monthlyTop100MVUpdateTasklet(@Value("#{jobParameters['date']}") String date) {
    return (contribution, chunkContext) -> {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      log.info("Starting monthly TOP 100 MV update...");

      String currentDate = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      String yearMonth = currentDate.substring(0, 6); // YYYYMM 추출

      // 해당 월의 시작일과 마지막일 계산
      String startDate = yearMonth + "01"; // 해당 월 1일
      String endDate = yearMonth + "31";   // 해당 월 31일 (31일이 없는 달도 포함)

      // 기존 MV 데이터 삭제
      jdbcTemplate.update("DELETE FROM mv_product_rank_monthly WHERE period_yyyymm = ?", yearMonth);
      log.info("Cleared existing monthly MV data for period: {}", yearMonth);

      // Daily 테이블에서 월간 집계하여 TOP 100 데이터를 MV 테이블에 삽입
      String insertSQL = """
          INSERT INTO mv_product_rank_monthly (
              product_id, period_yyyymm, ranking, score,
              like_count, order_count, view_count, created_at, updated_at  
          )
          SELECT 
              product_id,
              ? as period_yyyymm,
              ROW_NUMBER() OVER (ORDER BY (0.1 * SUM(view_count) + 0.2 * SUM(like_count) + 0.6 * SUM(order_count)) DESC) as ranking,
              (0.1 * SUM(view_count) + 0.2 * SUM(like_count) + 0.6 * SUM(order_count)) as score,
              SUM(like_count) as like_count,
              SUM(order_count) as order_count,
              SUM(view_count) as view_count, 
              NOW(),
              NOW()
          FROM product_metrics_daily
          WHERE period_yyyymmdd >= ? AND period_yyyymmdd <= ?
          GROUP BY product_id
          ORDER BY score DESC
          LIMIT 100
          """;

      int insertedCount = jdbcTemplate.update(insertSQL, yearMonth, startDate, endDate);
      log.info("Inserted {} records into mv_product_rank_monthly for period: {} (from {} to {})",
          insertedCount, yearMonth, startDate, endDate);

      // Redis 캐시 삭제
      String monthlyPattern = "ranking:monthly:" + yearMonth + "M:*";
      deleteRedisCacheByPattern(monthlyPattern);
      log.info("Cleared Redis cache for monthly pattern: {}", monthlyPattern);

      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Step monthlyTop100MVUpdateStep() {
    return new StepBuilder("monthlyTop100MVUpdateStep", jobRepository)
        .tasklet(monthlyTop100MVUpdateTasklet(null), transactionManager)
        .build();
  }

  private void deleteRedisCacheByPattern(String pattern) {
    try {
      var keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.debug("Deleted {} cache keys matching pattern: {}", keys.size(), pattern);
      }
    } catch (Exception e) {
      log.warn("Failed to clear Redis cache for pattern: {}", pattern, e);
    }
  }
}
