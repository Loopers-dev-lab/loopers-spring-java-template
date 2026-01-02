package com.loopers.application.ranking.batch;

import com.loopers.domain.metrics.product.ProductMetricsDailyAggregated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ItemReader<ProductMetricsDailyAggregated> productMetricsDailyReader;
    private final RankingProcessor rankingProcessor;
    private final RankingStepExecutionListener rankingStepExecutionListener;

    @Bean
    public Step rankingChunkStep() {
        return new StepBuilder("rankingChunkStep", jobRepository)
                .<ProductMetricsDailyAggregated, ProductMetricsDailyAggregated>chunk(1000, transactionManager)
                .reader(productMetricsDailyReader)
                .processor(rankingProcessor)
                .writer(chunk -> {
                    if (!chunk.isEmpty()) {
                        log.debug("Writing ranking chunk of size: {}", chunk.size());
                    }
                })
                .listener(rankingStepExecutionListener)
                .build();
    }
}
