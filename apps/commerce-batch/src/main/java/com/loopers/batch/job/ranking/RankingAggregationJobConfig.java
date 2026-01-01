package com.loopers.batch.job.ranking;

import com.loopers.batch.config.RankingBatchProperties;
import com.loopers.batch.domain.ranking.MonthlyProductRank;
import com.loopers.batch.domain.ranking.ProductRankEntity;
import com.loopers.batch.domain.ranking.RankingPeriod;
import com.loopers.batch.domain.ranking.WeeklyProductRank;
import com.loopers.batch.listener.JobListener;
import com.loopers.infrastructure.ranking.MonthlyProductRankJpaRepository;
import com.loopers.infrastructure.ranking.WeeklyProductRankJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingAggregationJobConfig {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String JOB_NAME = "rankingAggregationJob";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final EntityManagerFactory entityManagerFactory;
  private final RankingBatchProperties properties;
  private final JobListener jobListener;
  private final WeeklyProductRankJpaRepository weeklyRepository;
  private final MonthlyProductRankJpaRepository monthlyRepository;

  @Bean(JOB_NAME)
  public Job rankingAggregationJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .listener(jobListener)
        .start(rankingAggregationStep(null, null))
        .build();
  }

  @Bean
  @JobScope
  public Step rankingAggregationStep(
      @Value("#{jobParameters['period']}") String period,
      @Value("#{jobParameters['baseDate']}") String baseDate) {
    return new StepBuilder("rankingAggregationStep", jobRepository)
        .<AggregatedProductScore, ProductRankEntity>chunk(properties.getBatch().getChunkSize(), transactionManager)
        .reader(aggregatedScoreReader(period, baseDate))
        .processor(rankingProcessor())
        .writer(rankingWriter(period))
        .build();
  }

  @Bean
  @StepScope
  public JpaPagingItemReader<AggregatedProductScore> aggregatedScoreReader(
      @Value("#{jobParameters['period']}") String period,
      @Value("#{jobParameters['baseDate']}") String baseDate) {

    LocalDate base = LocalDate.parse(baseDate, DATE_FORMATTER);
    RankingPeriod rankingPeriod = RankingPeriod.fromCode(period);
    DateRange dateRange = calculateDateRange(rankingPeriod, base);

    log.info("집계 기간: {} ~ {} (period={})", dateRange.startDate(), dateRange.endDate(), period);

    String jpql = """
        SELECT new com.loopers.batch.job.ranking.AggregatedProductScore(
            m.id.refProductId,
            SUM(m.viewCount),
            SUM(m.likeCount),
            SUM(m.salesCount)
        )
        FROM ProductMetrics m
        WHERE m.id.metricDate BETWEEN :startDate AND :endDate
        GROUP BY m.id.refProductId
        ORDER BY
          (CAST(SUM(m.viewCount) AS double) * :viewWeight
            + CAST(SUM(m.likeCount) AS double) * :likeWeight
            + CAST(SUM(m.salesCount) AS double) * :orderWeight) DESC,
          m.id.refProductId
        """;

    Map<String, Object> params = new HashMap<>();
    params.put("startDate", dateRange.startDate());
    params.put("endDate", dateRange.endDate());
    params.put("viewWeight", properties.getWeight().getView());
    params.put("likeWeight", properties.getWeight().getLike());
    params.put("orderWeight", properties.getWeight().getOrder());

    return new JpaPagingItemReaderBuilder<AggregatedProductScore>()
        .name("aggregatedScoreReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString(jpql)
        .parameterValues(params)
        .pageSize(properties.getBatch().getChunkSize())
        .maxItemCount(properties.getBatch().getLimit())
        .build();
  }

  @Bean
  @StepScope
  public RankingItemProcessor rankingProcessor() {
    return new RankingItemProcessor(properties);
  }

  @Bean
  @StepScope
  public ItemWriter<ProductRankEntity> rankingWriter(
      @Value("#{jobParameters['period']}") String period) {
    RankingPeriod rankingPeriod = RankingPeriod.fromCode(period);

    if (rankingPeriod == RankingPeriod.WEEKLY) {
      return items -> {
        List<WeeklyProductRank> ranks = items.getItems().stream()
            .map(WeeklyProductRank.class::cast)
            .toList();
        weeklyRepository.saveAll(ranks);
        log.debug("주간 점수 {} 건 저장 완료", ranks.size());
      };
    }
    return items -> {
      List<MonthlyProductRank> ranks = items.getItems().stream()
          .map(MonthlyProductRank.class::cast)
          .toList();
      monthlyRepository.saveAll(ranks);
      log.debug("월간 점수 {} 건 저장 완료", ranks.size());
    };
  }

  private DateRange calculateDateRange(RankingPeriod period, LocalDate base) {
    if (period == RankingPeriod.WEEKLY) {
      LocalDate weekStart = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      LocalDate weekEnd = base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
      return DateRange.of(
          Integer.parseInt(weekStart.format(DATE_FORMATTER)),
          Integer.parseInt(weekEnd.format(DATE_FORMATTER)));
    }
    LocalDate monthStart = base.withDayOfMonth(1);
    LocalDate monthEnd = base.with(TemporalAdjusters.lastDayOfMonth());
    return DateRange.of(
        Integer.parseInt(monthStart.format(DATE_FORMATTER)),
        Integer.parseInt(monthEnd.format(DATE_FORMATTER)));
  }
}
