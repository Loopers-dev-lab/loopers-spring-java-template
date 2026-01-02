package com.loopers.batch.job.ranking;

import com.loopers.batch.job.ranking.step.weekly.WeeklyRankingProcessor;
import com.loopers.domain.ProductMetrics;
import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RankingChunkConfig {

  private final WeeklyRankingProcessor weeklyRankingProcessor;
  private final EntityManagerFactory emf;

  @Bean
  @StepScope
  public JpaPagingItemReader<ProductMetrics> rankingReader() {
    return new JpaPagingItemReaderBuilder<ProductMetrics>()
        .name("rankingReader")
        .entityManagerFactory(emf)
        .queryString("SELECT m FROM ProductMetrics m WHERE m.updatedAt >= :startDate")
        .parameterValues(Map.of("startDate", LocalDateTime.now().minusDays(7)))
        .pageSize(100)
        .build();
  }

  @Bean
  public ItemProcessor<ProductMetrics, WeeklyRankingWork> rankingProcessor() {
    return weeklyRankingProcessor;
  }

  @Bean
  @StepScope
  public JpaItemWriter<WeeklyRankingWork> rankingWriter() {
    return new JpaItemWriterBuilder<WeeklyRankingWork>()
        .entityManagerFactory(emf)
        .build();
  }

  @Bean
  @StepScope
  public JpaPagingItemReader<ProductMetrics> monthlyRankingReader(
      @Value("#{jobParameters['startDate']}") String startDate
  ) {
    return new JpaPagingItemReaderBuilder<ProductMetrics>()
        .name("monthlyRankingReader")
        .entityManagerFactory(emf)
        .queryString("SELECT m FROM ProductMetrics m WHERE m.updatedAt >= :startDate")
        .parameterValues(Map.of("startDate", LocalDateTime.parse(startDate)))
        .pageSize(100)
        .build();
  }
}
