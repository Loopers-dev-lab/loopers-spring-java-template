package com.loopers.job.ranking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EnableJpaRepositories(basePackageClasses = ProductMetricsTestRepository.class)
public class RankingJobTestConfig {
}
