package com.loopers.batch.metrics.job;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsDailyRepository;
import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
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
class ProductMetricsMonthlyJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private ProductMetricsDailyRepository dailyRepository;

    @Autowired
    private ProductMetricsMonthlyRepository monthlyRepository;

    @Autowired
    private Job productMetricsMonthlyJob;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(productMetricsMonthlyJob);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("월간 집계 배치가 성공적으로 실행되고 Monthly 데이터가 생성된다")
    void productMetricsMonthlyJob_Success() throws Exception {
        // given: 2025년 12월 (2025-12-01 ~ 2025-12-31) Daily 데이터 생성
        int year = 2025;
        int month = 12;
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        // 상품 3개에 대해 31일치 Daily 데이터 생성
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
                .addString("month", String.valueOf(month))
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: Job 성공 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Step 실행 결과 확인
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("aggregateMonthlyMetricsStep");
        assertThat(stepExecution.getReadCount()).isEqualTo(3);  // 3개 상품
        assertThat(stepExecution.getWriteCount()).isEqualTo(3);
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 실제 DB 저장 결과 확인
        List<ProductMetricsMonthly> monthlyMetrics = monthlyRepository.findAll();
        assertThat(monthlyMetrics).hasSize(3);

        // 첫 번째 상품의 월간 집계 검증
        ProductMetricsMonthly firstProduct = monthlyMetrics.stream()
                .filter(m -> m.getProductId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(firstProduct.getYear()).isEqualTo(year);
        assertThat(firstProduct.getMonth()).isEqualTo(month);
        assertThat(firstProduct.getPeriodStartDate()).isEqualTo(startDate);
        assertThat(firstProduct.getPeriodEndDate()).isEqualTo(endDate);
        assertThat(firstProduct.getTotalLikeCount()).isEqualTo(310L);  // 10 * 31일
        assertThat(firstProduct.getTotalViewCount()).isEqualTo(3100L); // 100 * 31일
        assertThat(firstProduct.getTotalOrderCount()).isEqualTo(155L); // 5 * 31일
        assertThat(firstProduct.getAggregatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Daily 데이터가 없으면 월간 집계가 생성되지 않는다")
    void productMetricsMonthlyJob_NoData() throws Exception {
        // given: Daily 데이터 없음
        int year = 2025;
        int month = 12;

        // when: Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("month", String.valueOf(month))
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: Job은 성공하지만 처리 데이터 없음
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(0);

        // Monthly 데이터 생성되지 않음
        List<ProductMetricsMonthly> monthlyMetrics = monthlyRepository.findAll();
        assertThat(monthlyMetrics).isEmpty();
    }

    @Test
    @DisplayName("동일한 월간 집계를 다시 실행하면 UPSERT로 업데이트된다")
    void productMetricsMonthlyJob_Upsert() throws Exception {
        // given: 2025년 12월 Daily 데이터 생성
        int year = 2025;
        int month = 12;
        LocalDate startDate = LocalDate.of(2025, 12, 1);

        ProductMetricsDaily daily = ProductMetricsDaily.create(1L, startDate);
        daily.addLikeDelta(10);
        daily.addViewDelta(100);
        daily.addOrderDelta(5);
        dailyRepository.save(daily);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("month", String.valueOf(month))
                .toJobParameters();

        // when: 첫 번째 실행
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<ProductMetricsMonthly> firstResult = monthlyRepository.findAll();
        assertThat(firstResult).hasSize(1);
        assertThat(firstResult.get(0).getTotalLikeCount()).isEqualTo(10L);

        // Daily 데이터 변경 (증가)
        ProductMetricsDaily updatedDaily = dailyRepository
                .findByProductIdAndMetricDate(1L, startDate)
                .orElseThrow();
        updatedDaily.addLikeDelta(20); // 추가로 20 증가
        dailyRepository.save(updatedDaily);

        // when: 동일한 월로 두 번째 실행 (새로운 timestamp로)
        JobParameters secondJobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis() + 1000)
                .addString("year", String.valueOf(year))
                .addString("month", String.valueOf(month))
                .toJobParameters();

        JobExecution secondExecution = jobLauncherTestUtils.launchJob(secondJobParameters);

        // then: Job 성공
        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Monthly 데이터는 여전히 1개 (UPSERT)
        List<ProductMetricsMonthly> secondResult = monthlyRepository.findAll();
        assertThat(secondResult).hasSize(1);

        // 값이 업데이트됨 (10 + 20 = 30)
        assertThat(secondResult.get(0).getTotalLikeCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("특정 Step만 실행할 수 있다")
    void aggregateMonthlyMetricsStep_Success() {
        // given: 테스트 데이터
        ProductMetricsDaily daily = ProductMetricsDaily.create(1L, LocalDate.of(2025, 12, 1));
        daily.addLikeDelta(50);
        dailyRepository.save(daily);

        // JobParameters 생성 (StepScope 빈에 필요)
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", "2025")
                .addString("month", "12")
                .toJobParameters();

        // when: Step만 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("aggregateMonthlyMetricsStep", jobParameters);

        // then: Step 성공
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("aggregateMonthlyMetricsStep");
    }

    @Test
    @DisplayName("2월(28일)과 12월(31일)의 일수 차이를 정확히 처리한다")
    void productMetricsMonthlyJob_DifferentMonthDays() throws Exception {
        // given: 2025년 2월 (평년, 28일)
        int year = 2025;
        int month = 2;
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28); // 2025년은 평년

        // 상품 1개에 대해 2월 전체 Daily 데이터 생성
        List<ProductMetricsDaily> dailyMetrics = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ProductMetricsDaily daily = ProductMetricsDaily.create(1L, date);
            daily.addLikeDelta(10);
            dailyMetrics.add(daily);
        }
        dailyRepository.saveAll(dailyMetrics);

        // when: Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("year", String.valueOf(year))
                .addString("month", String.valueOf(month))
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: Job 성공
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<ProductMetricsMonthly> monthlyMetrics = monthlyRepository.findAll();
        assertThat(monthlyMetrics).hasSize(1);

        ProductMetricsMonthly result = monthlyMetrics.get(0);
        assertThat(result.getYear()).isEqualTo(year);
        assertThat(result.getMonth()).isEqualTo(month);
        assertThat(result.getPeriodStartDate()).isEqualTo(startDate);
        assertThat(result.getPeriodEndDate()).isEqualTo(endDate);
        assertThat(result.getTotalLikeCount()).isEqualTo(280L);  // 10 * 28일 (평년)
    }
}
