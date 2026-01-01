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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
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
  private final DataSource dataSource;
  private final EntityManagerFactory entityManagerFactory;
  private final RedisTemplate<String, String> redisTemplate;

  @Bean
  public Job weeklyRankingMVUpdateJob() {
    return new JobBuilder("weeklyRankingMVUpdateJob", jobRepository)
        .start(weeklyTop100MVUpdateStep())
        .build();
  }


  private String getCurrentYearMonthWeek() {
    LocalDate now = LocalDate.now();
    WeekFields weekFields = WeekFields.of(Locale.getDefault());
    int weekOfYear = now.get(weekFields.weekOfYear());
    return now.format(DateTimeFormatter.ofPattern("yyyy")) + String.format("%02d", weekOfYear);
  }


  @Bean
  @StepScope
  public Tasklet weeklyTop100MVUpdateTasklet(@Value("#{jobParameters['date']}") String date) {
    return (contribution, chunkContext) -> {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      log.info("Starting weekly TOP 100 MV update...");

      String currentDate = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      LocalDate targetDate = LocalDate.parse(currentDate, DateTimeFormatter.ofPattern("yyyyMMdd"));

      // 해당 주의 월요일과 일요일 계산
      LocalDate weekStart = targetDate.with(java.time.DayOfWeek.MONDAY);
      LocalDate weekEnd = targetDate.with(java.time.DayOfWeek.SUNDAY);

      String startDate = weekStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      String endDate = weekEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      String yearMonthWeek = getCurrentYearMonthWeek();

      // 기존 MV 데이터 삭제
      jdbcTemplate.update("DELETE FROM mv_product_rank_weekly WHERE period_yyyyww = ?", yearMonthWeek);
      log.info("Cleared existing weekly MV data for period: {}", yearMonthWeek);

      // Daily 테이블에서 주간 집계하여 TOP 100 데이터를 MV 테이블에 삽입
      String insertSQL = """
          INSERT INTO mv_product_rank_weekly (
              product_id, period_yyyyww, ranking, score, 
              like_count, order_count, view_count, created_at, updated_at
          )
          SELECT 
              product_id,
              ? as period_yyyyww,
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

      int insertedCount = jdbcTemplate.update(insertSQL, yearMonthWeek, startDate, endDate);
      log.info("Inserted {} records into mv_product_rank_weekly for period: {} (from {} to {})",
          insertedCount, yearMonthWeek, startDate, endDate);

      // Redis 캐시 삭제
      String weeklyPattern = "ranking:weekly:" + yearMonthWeek + "W:*";
      deleteRedisCacheByPattern(weeklyPattern);
      log.info("Cleared Redis cache for weekly pattern: {}", weeklyPattern);

      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Step weeklyTop100MVUpdateStep() {
    return new StepBuilder("weeklyTop100MVUpdateStep", jobRepository)
        .tasklet(weeklyTop100MVUpdateTasklet(null), transactionManager)
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
