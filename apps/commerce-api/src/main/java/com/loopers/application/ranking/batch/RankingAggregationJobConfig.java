package com.loopers.application.ranking.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RankingAggregationJobConfig {

    private final JobRepository jobRepository;
    private final Step rankingChunkStep;
    private final JobExecutionListener rankingJobExecutionListener;

    @Bean
    public Job rankingJob() {
        return new JobBuilder("rankingJob", jobRepository)
                .start(rankingChunkStep)
                .listener(rankingJobExecutionListener)
                .build();
    }
}
