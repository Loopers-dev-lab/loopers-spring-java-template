package com.loopers.batch.job.productRankingJob.step;

import com.loopers.batch.job.productRankingJob.step.processor.RankingScoreProcessor;
import com.loopers.batch.job.productRankingJob.step.reader.RankingScoreReader;
import com.loopers.batch.job.productRankingJob.step.writer.WeeklyRankingWriter;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.WeeklyProductRank;
import com.loopers.domain.rank.WeeklyRankRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import com.loopers.domain.rank.ProductRankingAggregation;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyRankingStepConfig {

    private static final int CHUNK_SIZE = 100;

    private final EntityManager entityManager;
    private final WeeklyRankRepository weeklyRankRepository;

    @Bean
    @JobScope
    public Step weeklyRankingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
			@Value("#{jobParameters['anchorDate']}") String anchorDate
    ) {
		log.info("Initializing weeklyRankingStep: anchorDate={}", anchorDate);

        return new StepBuilder("weeklyRankingStep", jobRepository)
                .<ProductRankingAggregation, WeeklyProductRank>chunk(CHUNK_SIZE, transactionManager)
				.reader(weeklyRankingReader(null))
				.processor(weeklyRankingProcessor(null))
                .writer(weeklyRankingWriter(null))
                .build();
    }

    @Bean
    @StepScope
	public ItemReader<ProductRankingAggregation> weeklyRankingReader(
			@Value("#{jobParameters['anchorDate']}") String anchorDate
    ) {
        return new RankingScoreReader(
                entityManager,
				anchorDate,
                "WEEKLY"
        );
    }

    @Bean
    @StepScope
    public ItemProcessor<ProductRankingAggregation, WeeklyProductRank> weeklyRankingProcessor(
            @Value("#{jobParameters['anchorDate']}") String anchorDate
    ) {
        RankingScoreProcessor processor = new RankingScoreProcessor("WEEKLY", anchorDate);
        return item -> (WeeklyProductRank) processor.process(item);
    }

    @Bean
    @StepScope
    public ItemWriter<WeeklyProductRank> weeklyRankingWriter(
            @Value("#{jobParameters['anchorDate']}") String anchorDate
    ) {
        return new WeeklyRankingWriter(weeklyRankRepository, java.time.LocalDate.parse(anchorDate));
    }
}
