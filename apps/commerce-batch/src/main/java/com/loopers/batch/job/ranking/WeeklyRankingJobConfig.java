package com.loopers.batch.job.ranking;

import com.loopers.batch.job.ranking.step.RankingPrepareTasklet;
import com.loopers.batch.job.ranking.step.RankingTableSwapTasklet;
import com.loopers.batch.listener.JobListener;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = WeeklyRankingJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class WeeklyRankingJobConfig {

  public static final String JOB_NAME = "weeklyRankingJob";

  private final JobRepository jobRepository;
  private final JobListener jobListener;
  private final PlatformTransactionManager transactionManager;

  private final RankingPrepareTasklet prepareTasklet;
  private final RankingTableSwapTasklet tableSwapTasklet;

  private final ItemReader<ProductMetrics> rankingReader;
  private final ItemProcessor<ProductMetrics, WeeklyRankingWork> rankingProcessor;
  private final ItemWriter<WeeklyRankingWork> rankingWriter;

  @Bean(JOB_NAME)
  public Job weeklyRankingJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(prepareStep())
        .next(calculationStep())
        .next(tableSwapStep())
        .listener(jobListener)
        .build();
  }

  @Bean
  public Step prepareStep() {
    return new StepBuilder("prepareStep", jobRepository)
        .tasklet(prepareTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step calculationStep() {
    return new StepBuilder("calculationStep", jobRepository)
        .<ProductMetrics, WeeklyRankingWork>chunk(100, transactionManager)
        .reader(rankingReader)
        .processor(rankingProcessor)
        .writer(rankingWriter)
        .build();
  }

  @Bean
  public Step tableSwapStep() {
    return new StepBuilder("tableSwapStep", jobRepository)
        .tasklet(tableSwapTasklet, transactionManager)
        .build();
  }
}
