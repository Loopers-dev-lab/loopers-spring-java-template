package com.loopers.batch.job.productRankingJob.step;

import com.loopers.batch.job.productRankingJob.step.processor.RankingScoreProcessor;
import com.loopers.batch.job.productRankingJob.step.reader.RankingScoreReader;
import com.loopers.batch.job.productRankingJob.step.writer.MonthlyRankingWriter;
import com.loopers.domain.rank.ProductRankingAggregation;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;

@Configuration
@RequiredArgsConstructor
public class MonthlyRankingStepConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;

    private final EntityManager entityManager;
    private final MonthlyRankRepository monthlyRankRepository;

    @Bean
    @JobScope
    public Step monthlyRankingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("monthlyRankingStep", jobRepository)
                .<ProductRankingAggregation, MonthlyProductRank>chunk(CHUNK_SIZE, transactionManager)
				.reader(monthlyRankingReader(null))
				.processor(monthlyRankingProcessor(null))
                .writer(monthlyRankingWriter(null))
                .build();
    }

    @Bean
	@StepScope
	public ItemReader<ProductRankingAggregation> monthlyRankingReader(
			@org.springframework.beans.factory.annotation.Value("#{jobParameters['anchorDate']}")
			String anchorDate
    ) {
        return new RankingScoreReader(
                entityManager,
				anchorDate,
                "MONTHLY"
        );
    }

    @Bean
	@StepScope
	public ItemProcessor<ProductRankingAggregation, MonthlyProductRank> monthlyRankingProcessor(
			@org.springframework.beans.factory.annotation.Value("#{jobParameters['anchorDate']}")
			String anchorDate
    ) {
        RankingScoreProcessor processor =
				new RankingScoreProcessor("MONTHLY", anchorDate);

		return item -> (MonthlyProductRank) processor.process(item);
    }

	@Bean
	@StepScope
	public ItemWriter<MonthlyProductRank> monthlyRankingWriter(
			@org.springframework.beans.factory.annotation.Value("#{jobParameters['anchorDate']}")
			String anchorDate
	) {
		return new MonthlyRankingWriter(monthlyRankRepository, java.time.LocalDate.parse(anchorDate));
	}
}
