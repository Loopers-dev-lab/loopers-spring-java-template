package com.loopers.batch.job.ranking;

import com.loopers.batch.job.ranking.step.RankingPrepareTasklet;
import com.loopers.batch.job.ranking.step.monthly.MonthlyRankingTableSwapTasklet;
import com.loopers.domain.ProductMetrics;
import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {
  public static final String JOB_NAME = "monthlyRankingJob";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RankingPrepareTasklet prepareTasklet;
  private final MonthlyRankingTableSwapTasklet tableSwapTasklet; // 월간 전용 스왑

  private final JpaPagingItemReader<ProductMetrics> monthlyRankingReader;
  private final ItemProcessor<ProductMetrics, WeeklyRankingWork> rankingProcessor;
  private final JpaItemWriter<WeeklyRankingWork> rankingWriter;

  @Bean(JOB_NAME)
  public Job monthlyRankingJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(monthlyPrepareStep())
        .next(monthlyCalculationStep())
        .next(monthlyTableSwapStep())
        .build();
  }

  @Bean
  public Step monthlyPrepareStep() {
    return new StepBuilder("monthlyPrepareStep", jobRepository)
        .tasklet(prepareTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step monthlyCalculationStep() {
    return new StepBuilder("monthlyCalculationStep", jobRepository)
        .<ProductMetrics, WeeklyRankingWork>chunk(100, transactionManager)
        .reader(monthlyRankingReader) // 기간을 30일로 설정한 Reader
        .processor(rankingProcessor)
        .writer(rankingWriter)
        .build();
  }

  @Bean
  public Step monthlyTableSwapStep() {
    return new StepBuilder("monthlyTableSwapStep", jobRepository)
        .tasklet(tableSwapTasklet, transactionManager)
        .build();
  }
}
