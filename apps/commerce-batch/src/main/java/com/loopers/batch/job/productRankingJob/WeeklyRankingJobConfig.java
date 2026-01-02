package com.loopers.batch.job.productRankingJob;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeeklyRankingJobConfig {

    @Bean
    public Job weeklyRankingJob(
            JobRepository jobRepository,
            @Qualifier("weeklyRankingStep") Step weeklyRankingStep
    ) {
        return new JobBuilder("weeklyRankingJob", jobRepository)
                .start(weeklyRankingStep)
                .build();
    }
}
