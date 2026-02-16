package com.loopers.batch.metrics.job;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsDailyRepository;
import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsWeeklyRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = {
        "spring.batch.job.enabled=false",
        "spring.batch.jdbc.initialize-schema=always"
})
class ProductMetricsWeeklyJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private ProductMetricsDailyRepository dailyRepository;

    @Autowired
    private ProductMetricsWeeklyRepository weeklyRepository;

    @Autowired
    private Job productMetricsWeeklyJob;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(productMetricsWeeklyJob);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("주간 집계 배치가 성공적으로 실행되고 Weekly 데이터가 생성된다")
    void productMetricsWeeklyJob_Success() throws Exception {
        // given: 2025년 12월 1주차 (2025-12-01 ~ 2025-12-07) Daily 데이터 생성
        int year = 2025;
        int week = 49;
        LocalDate startDate = LocalDate.of(2025, 12, 1); // 월요일
        LocalDate endDate = LocalDate.of(2025, 12, 7);   // 일요일

        // 상품 3개에 대해 7일치 Daily 데이터 생성
        List<ProductMetricsDaily> dailyMetrics = new ArrayList<>();
        for (long productId = 1L; productId <= 3L; productId++) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                ProductMetricsDaily daily = ProductMetricsDaily.create(productId, date);
                daily.addLikeDelta(10);  // 일일 좋아요 10개
                daily.addViewDelta(100); // 일일 조회 100개
                daily.addOrderDelta(5);  // 일일 주문 5개
                dailyMetrics.add(daily);
            }
        }
        dailyRepository.saveAll(dailyMetrics);

        // when: Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("week", String.valueOf(week))
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: Job 성공 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Step 실행 결과 확인
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("aggregateWeeklyMetricsStep");
        assertThat(stepExecution.getReadCount()).isEqualTo(3);  // 3개 상품
        assertThat(stepExecution.getWriteCount()).isEqualTo(3);
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 실제 DB 저장 결과 확인
        List<ProductMetricsWeekly> weeklyMetrics = weeklyRepository.findAll();
        assertThat(weeklyMetrics).hasSize(3);

        // 첫 번째 상품의 주간 집계 검증
        ProductMetricsWeekly firstProduct = weeklyMetrics.stream()
                .filter(m -> m.getProductId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(firstProduct.getYear()).isEqualTo(year);
        assertThat(firstProduct.getWeek()).isEqualTo(week);
        assertThat(firstProduct.getPeriodStartDate()).isEqualTo(startDate);
        assertThat(firstProduct.getPeriodEndDate()).isEqualTo(endDate);
        assertThat(firstProduct.getTotalLikeCount()).isEqualTo(70L);  // 10 * 7일
        assertThat(firstProduct.getTotalViewCount()).isEqualTo(700L); // 100 * 7일
        assertThat(firstProduct.getTotalOrderCount()).isEqualTo(35L); // 5 * 7일
        assertThat(firstProduct.getAggregatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Daily 데이터가 없으면 주간 집계가 생성되지 않는다")
    void productMetricsWeeklyJob_NoData() throws Exception {
        // given: Daily 데이터 없음
        int year = 2025;
        int week = 49;

        // when: Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("week", String.valueOf(week))
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: Job은 성공하지만 처리 데이터 없음
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(0);

        // Weekly 데이터 생성되지 않음
        List<ProductMetricsWeekly> weeklyMetrics = weeklyRepository.findAll();
        assertThat(weeklyMetrics).isEmpty();
    }

    @Test
    @DisplayName("동일한 주간 집계를 다시 실행하면 UPSERT로 업데이트된다")
    void productMetricsWeeklyJob_Upsert() throws Exception {
        // given: 2025년 12월 1주차 Daily 데이터 생성
        int year = 2025;
        int week = 49;
        LocalDate startDate = LocalDate.of(2025, 12, 1);

        ProductMetricsDaily daily = ProductMetricsDaily.create(1L, startDate);
        daily.addLikeDelta(10);
        daily.addViewDelta(100);
        daily.addOrderDelta(5);
        dailyRepository.save(daily);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("week", String.valueOf(week))
                .toJobParameters();

        // when: 첫 번째 실행
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<ProductMetricsWeekly> firstResult = weeklyRepository.findAll();
        assertThat(firstResult).hasSize(1);
        assertThat(firstResult.get(0).getTotalLikeCount()).isEqualTo(10L);

        // Daily 데이터 변경 (증가)
        ProductMetricsDaily updatedDaily = dailyRepository
                .findByProductIdAndMetricDate(1L, startDate)
                .orElseThrow();
        updatedDaily.addLikeDelta(20); // 추가로 20 증가
        dailyRepository.save(updatedDaily);

        // when: 동일한 주차로 두 번째 실행 (새로운 timestamp로)
        JobParameters secondJobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis() + 1000)
                .addString("year", String.valueOf(year))
                .addString("week", String.valueOf(week))
                .toJobParameters();

        JobExecution secondExecution = jobLauncherTestUtils.launchJob(secondJobParameters);

        // then: Job 성공
        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Weekly 데이터는 여전히 1개 (UPSERT)
        List<ProductMetricsWeekly> secondResult = weeklyRepository.findAll();
        assertThat(secondResult).hasSize(1);

        // 값이 업데이트됨 (10 + 20 = 30)
        assertThat(secondResult.get(0).getTotalLikeCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("특정 Step만 실행할 수 있다")
    void aggregateWeeklyMetricsStep_Success() {
        // given: 테스트 데이터
        ProductMetricsDaily daily = ProductMetricsDaily.create(1L, LocalDate.of(2025, 12, 1));
        daily.addLikeDelta(50);
        dailyRepository.save(daily);

        // JobParameters 생성 (StepScope 빈에 필요)
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", "2025")
                .addString("week", "49")
                .toJobParameters();

        // when: Step만 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("aggregateWeeklyMetricsStep", jobParameters);

        // then: Step 성공
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("aggregateWeeklyMetricsStep");
    }
}