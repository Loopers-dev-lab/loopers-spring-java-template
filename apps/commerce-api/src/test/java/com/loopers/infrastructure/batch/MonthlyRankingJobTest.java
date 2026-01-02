package com.loopers.infrastructure.batch;

import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.ProductRankMonthly;
import com.loopers.infrastructure.ranking.ProductRankMonthlyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.core.BatchStatus.COMPLETED;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class MonthlyRankingJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("monthlyRankingJob")
    private Job monthlyRankingJob;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private ProductRankMonthlyRepository productRankMonthlyRepository;

    @BeforeEach
    void setUp() {
        productRankMonthlyRepository.deleteAll();
        productMetricsRepository.deleteAll();
        jobLauncherTestUtils.setJob(monthlyRankingJob);
    }

    @Test
    @DisplayName("월간 랭킹 Job 실행 시 product_metrics를 집계하여 mv_product_rank_monthly에 저장한다")
    void monthlyRankingJobTest() throws Exception {
        // given
        LocalDate jan1 = LocalDate.of(2025, 1, 1);
        LocalDate jan15 = LocalDate.of(2025, 1, 15);
        LocalDate jan31 = LocalDate.of(2025, 1, 31);

        // 상품 101: 1월 전체 viewCount=150, likeCount=15, orderCount=7
        // score = 150 + (15 * 2) + (7 * 6) = 150 + 30 + 42 = 222
        productMetricsRepository.save(createMetrics(101L, jan1, 100L, 10L, 5L, 5L));
        productMetricsRepository.save(createMetrics(101L, jan15, 50L, 5L, 2L, 2L));

        // 상품 102: 1월 전체 viewCount=80, likeCount=30, orderCount=15
        // score = 80 + (30 * 2) + (15 * 6) = 80 + 60 + 90 = 230
        productMetricsRepository.save(createMetrics(102L, jan1, 30L, 10L, 5L, 5L));
        productMetricsRepository.save(createMetrics(102L, jan31, 50L, 20L, 10L, 10L));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "20250115")
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(COMPLETED);

        List<ProductRankMonthly> rankings = productRankMonthlyRepository
                .findByPeriodStartOrderByRankingAsc(jan1);

        assertThat(rankings).hasSize(2);

        // 1등: 상품 102 (score=230)
        assertThat(rankings.get(0).getProductId()).isEqualTo(102L);
        assertThat(rankings.get(0).getRanking()).isEqualTo(1);
        assertThat(rankings.get(0).getScore()).isEqualTo(230L);

        // 2등: 상품 101 (score=222)
        assertThat(rankings.get(1).getProductId()).isEqualTo(101L);
        assertThat(rankings.get(1).getRanking()).isEqualTo(2);
        assertThat(rankings.get(1).getScore()).isEqualTo(222L);
    }

    private ProductMetrics createMetrics(Long productId, LocalDate date,
                                          Long viewCount, Long likeCount,
                                          Long orderCount, Long totalQuantity) {
        ProductMetrics metrics = ProductMetrics.create(productId, date);
        ReflectionTestUtils.setField(metrics, "viewCount", viewCount);
        ReflectionTestUtils.setField(metrics, "likeCount", likeCount);
        ReflectionTestUtils.setField(metrics, "orderCount", orderCount);
        ReflectionTestUtils.setField(metrics, "totalQuantity", totalQuantity);
        return metrics;
    }
}
