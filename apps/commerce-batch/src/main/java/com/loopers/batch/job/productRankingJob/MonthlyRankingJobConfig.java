package com.loopers.batch.job.productRankingJob;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonthlyRankingJobConfig {

    @Bean
    public Job monthlyRankingJob(
            JobRepository jobRepository,
            @Qualifier("monthlyRankingStep") Step monthlyRankingStep
    ) {
        return new JobBuilder("monthlyRankingJob", jobRepository)
                .start(monthlyRankingStep)
                .build();
    }
}
