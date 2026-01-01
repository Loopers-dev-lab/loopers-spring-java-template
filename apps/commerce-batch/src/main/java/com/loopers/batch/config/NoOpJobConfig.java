package com.loopers.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * job.name이 지정되지 않았을 때 사용되는 빈 Job.
 * 아무 작업도 수행하지 않고 즉시 완료됨.
 */
@Configuration
@RequiredArgsConstructor
public class NoOpJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean("NONE")
  public Job noOpJob() {
    return new JobBuilder("NONE", jobRepository)
        .start(noOpStep())
        .build();
  }

  @Bean
  public Step noOpStep() {
    return new StepBuilder("noOpStep", jobRepository)
        .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
        .build();
  }
}
