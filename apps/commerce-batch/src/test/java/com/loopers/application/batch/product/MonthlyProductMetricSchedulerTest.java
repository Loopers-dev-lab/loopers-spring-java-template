package com.loopers.application.batch.product;

import com.loopers.application.batch.IntegrationTest;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.DailyProductMetricFixture;
import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("월간 상품 메트릭 배치")
class MonthlyProductMetricSchedulerTest extends IntegrationTest {

    @Autowired
    private DailyProductMetricRepository dailyProductMetricRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job monthlyProductMetricJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("배치 작업 실행")
    class BatchJobExecution {

        @Test
        @DisplayName("지난달 1일부터 말일까지의 일일 메트릭을 월간 메트릭으로 집계한다")
        void shouldAggregateMonthlyMetricFromDailyMetric() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2025, 1, 1);   // 1월 1일
            LocalDate endDate = LocalDate.of(2025, 1, 31);    // 1월 31일

            // 1월 전체 데이터 준비 (5개 상품 × 31일 = 155개 메트릭)
            List<DailyProductMetric> dailyMetrics = new ArrayList<>();
            for (int productNum = 1; productNum <= 5; productNum++) {
                for (int dayOffset = 0; dayOffset < 31; dayOffset++) {
                    LocalDateTime createdAt = startDate.plusDays(dayOffset).atStartOfDay();
                    DailyProductMetric metric = DailyProductMetricFixture.createWith(
                            String.valueOf(productNum),
                            10L + dayOffset,      // likeCount
                            20L + dayOffset,      // viewCount
                            30L + dayOffset,      // totalSalesCount
                            createdAt
                    );
                    dailyMetrics.add(metric);
                }
            }

            // 데이터 저장
            dailyProductMetricRepository.saveAll(dailyMetrics);

            // when
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(monthlyProductMetricJob, jobParameters);

            // then
            Integer savedMonthlyMetrics = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM monthly_product_metrics",
                    Integer.class
            );
            assertThat(savedMonthlyMetrics).isEqualTo(5);
        }

        @Test
        @DisplayName("파티션 배치가 올바르게 분할되어 동시 처리된다")
        void shouldPartitionDataCorrectly() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            // 100개 상품 × 31일 = 3100개 메트릭
            List<DailyProductMetric> dailyMetrics = new ArrayList<>();
            for (int productNum = 1; productNum <= 100; productNum++) {
                for (int dayOffset = 0; dayOffset < 31; dayOffset++) {
                    LocalDateTime createdAt = startDate.plusDays(dayOffset).atStartOfDay();
                    DailyProductMetric metric = DailyProductMetricFixture.createWith(
                            String.valueOf(productNum),
                            productNum,
                            (long) productNum * 2,
                            (long) productNum * 3,
                            createdAt
                    );
                    dailyMetrics.add(metric);
                }
            }

            // 데이터 저장
            dailyProductMetricRepository.saveAll(dailyMetrics);

            // when
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(monthlyProductMetricJob, jobParameters);

            // then - 100개 상품 모두 저장되었는지 확인
            Integer savedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM monthly_product_metrics",
                    Integer.class
            );
            assertThat(savedCount).isEqualTo(100);

            // 중복 없이 각 상품별 1개씩만 저장되었는지 확인
            Integer distinctProductCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT product_id) FROM monthly_product_metrics",
                    Integer.class
            );
            assertThat(distinctProductCount).isEqualTo(100);
        }

        @Test
        @DisplayName("메트릭 값이 올바르게 합산되어 저장된다")
        void shouldSumMetricsCorrectly() throws Exception {
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            // 상품 1: 31일 동안 좋아요 10씩 (총 310)
            List<DailyProductMetric> dailyMetrics = new ArrayList<>();
            for (int day = 0; day < 31; day++) {
                LocalDateTime createdAt = startDate.plusDays(day).atStartOfDay();
                DailyProductMetric metric = DailyProductMetricFixture.createWith("1", 10, 20, 30, createdAt);
                dailyMetrics.add(metric);
            }
            dailyProductMetricRepository.saveAll(dailyMetrics);

            // when
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(monthlyProductMetricJob, jobParameters);

            // then
            Long likeCount = jdbcTemplate.queryForObject(
                    "SELECT like_count FROM monthly_product_metrics WHERE product_id = 1",
                    Long.class
            );
            Long viewCount = jdbcTemplate.queryForObject(
                    "SELECT view_count FROM monthly_product_metrics WHERE product_id = 1",
                    Long.class
            );
            Long salesCount = jdbcTemplate.queryForObject(
                    "SELECT total_sales_count FROM monthly_product_metrics WHERE product_id = 1",
                    Long.class
            );

            assertThat(likeCount).isEqualTo(310L);   // 10 * 31
            assertThat(viewCount).isEqualTo(620L);   // 20 * 31
            assertThat(salesCount).isEqualTo(930L);  // 30 * 31
        }

        @Test
        @DisplayName("UPSERT가 정상 동작하여 중복 저장을 방지한다")
        void shouldHandleUpsertCorrectly() throws Exception {
            // given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 31);

            // 초기 데이터: 상품 1에 대한 31일 메트릭 저장
            List<DailyProductMetric> dailyMetrics = new ArrayList<>();
            for (int day = 0; day < 31; day++) {
                LocalDateTime createdAt = startDate.plusDays(day).atStartOfDay();
                DailyProductMetric metric = DailyProductMetricFixture.createWith("1", 10, 20, 30, createdAt);
                dailyMetrics.add(metric);
            }
            dailyProductMetricRepository.saveAll(dailyMetrics);

            // when - 첫 번째 배치 실행
            JobParameters jobParameters1 = new JobParametersBuilder()
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(monthlyProductMetricJob, jobParameters1);

            // 같은 데이터로 두 번째 배치 실행
            JobParameters jobParameters2 = new JobParametersBuilder()
                    .addString("startDate", startDate.toString())
                    .addString("endDate", endDate.toString())
                    .addLong("timestamp", System.currentTimeMillis() + 1000)
                    .toJobParameters();
            jobLauncher.run(monthlyProductMetricJob, jobParameters2);

            // then - 2개가 아닌 1개만 저장되어야 함
            Integer savedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM monthly_product_metrics WHERE product_id = 1",
                    Integer.class
            );
            assertThat(savedCount).isEqualTo(1);
        }
    }
}
