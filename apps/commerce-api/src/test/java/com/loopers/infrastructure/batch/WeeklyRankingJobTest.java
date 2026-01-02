package com.loopers.infrastructure.batch;

import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.ProductRankWeekly;
import com.loopers.infrastructure.ranking.ProductRankWeeklyRepository;
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
class WeeklyRankingJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("weeklyRankingJob")
    private Job weeklyRankingJob;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private ProductRankWeeklyRepository productRankWeeklyRepository;

    @BeforeEach
    void setUp() {
        productRankWeeklyRepository.deleteAll();
        productMetricsRepository.deleteAll();
        jobLauncherTestUtils.setJob(weeklyRankingJob);
    }

    @Test
    @DisplayName("주간 랭킹 Job 실행 시 product_metrics를 집계하여 mv_product_rank_weekly에 저장한다")
    void weeklyRankingJobTest() throws Exception {
        // given
        LocalDate monday = LocalDate.of(2025, 1, 6); // 2025-01-06 월요일

        // 상품 101: viewCount=100, likeCount=10, orderCount=5
        // score = 100 + (10 * 2) + (5 * 6) = 100 + 20 + 30 = 150
        productMetricsRepository.save(createMetrics(101L, monday, 100L, 10L, 5L, 5L));

        // 상품 102: viewCount=50, likeCount=20, orderCount=10
        // score = 50 + (20 * 2) + (10 * 6) = 50 + 40 + 60 = 150
        productMetricsRepository.save(createMetrics(102L, monday, 50L, 20L, 10L, 10L));

        // 상품 103: viewCount=200, likeCount=5, orderCount=2
        // score = 200 + (5 * 2) + (2 * 6) = 200 + 10 + 12 = 222
        productMetricsRepository.save(createMetrics(103L, monday.plusDays(1), 200L, 5L, 2L, 2L));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "20250106")
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(COMPLETED);

        List<ProductRankWeekly> rankings = productRankWeeklyRepository
                .findByPeriodStartOrderByRankingAsc(monday);

        assertThat(rankings).hasSize(3);

        // 1등: 상품 103 (score=222)
        assertThat(rankings.get(0).getProductId()).isEqualTo(103L);
        assertThat(rankings.get(0).getRanking()).isEqualTo(1);
        assertThat(rankings.get(0).getScore()).isEqualTo(222L);

        // 2등: 상품 101 (score=150, 동점이지만 product_id 순)
        assertThat(rankings.get(1).getProductId()).isEqualTo(101L);
        assertThat(rankings.get(1).getRanking()).isEqualTo(2);
        assertThat(rankings.get(1).getScore()).isEqualTo(150L);

        // 3등: 상품 102 (score=150)
        assertThat(rankings.get(2).getProductId()).isEqualTo(102L);
        assertThat(rankings.get(2).getRanking()).isEqualTo(3);
        assertThat(rankings.get(2).getScore()).isEqualTo(150L);
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
