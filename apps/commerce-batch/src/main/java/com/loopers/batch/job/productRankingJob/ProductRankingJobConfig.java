package com.loopers.batch.job.productRankingJob;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ProductRankingJobConfig {

	private final JobRepository jobRepository;
	private final Step weeklyRankingStep;
	private final Step monthlyRankingStep;

	@Bean
	public Job weeklyRankingJob() {
		return new JobBuilder("weeklyRankingJob", jobRepository)
				.start(weeklyRankingStep)
				.build();
	}

	@Bean
	public Job monthlyRankingJob() {
		return new JobBuilder("monthlyRankingJob", jobRepository)
				.start(monthlyRankingStep)
				.build();
	}
}


