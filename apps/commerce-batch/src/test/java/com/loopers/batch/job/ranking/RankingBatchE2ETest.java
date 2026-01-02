package com.loopers.batch.job.ranking;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 랭킹 배치 Job E2E 테스트
 * - 실제 Job 실행부터 MV 테이블 저장까지 전체 플로우 검증
 */
@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"  // 자동 실행 방지
})
@DisplayName("랭킹 배치 Job E2E 테스트")
class RankingBatchE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private WeeklyRankRepository weeklyRankRepository;

    @Autowired
    private MonthlyRankRepository monthlyRankRepository;

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        weeklyRankRepository.deleteByYearWeek("2024-W52");
        monthlyRankRepository.deleteByYearMonth("2024-12");
    }

    @Nested
    @DisplayName("주간 랭킹 배치 Job")
    class 주간_랭킹_배치_Job {

        @Test
        @DisplayName("주간 랭킹 Job이 성공적으로 실행되고 MV 테이블에 데이터가 저장된다")
        void should_execute_weekly_ranking_job_successfully() throws Exception {
            // given
            String yearWeek = "2024-W52";
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearWeek", yearWeek)
                    .addLong("timestamp", System.currentTimeMillis()) // 유니크 파라미터
                    .toJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // MV 테이블 검증
            List<WeeklyRankEntity> rankings = weeklyRankRepository.findByYearWeek(yearWeek);
            assertThat(rankings).isNotEmpty();
            assertThat(rankings.size()).isLessThanOrEqualTo(100); // TOP 100
            
            // 순위 검증
            if (!rankings.isEmpty()) {
                assertThat(rankings.get(0).getRankPosition()).isEqualTo(1);
                
                // 점수 순 정렬 검증
                for (int i = 1; i < rankings.size(); i++) {
                    assertThat(rankings.get(i-1).getTotalScore())
                            .isGreaterThanOrEqualTo(rankings.get(i).getTotalScore());
                }
            }
        }

        @Test
        @DisplayName("잘못된 yearWeek 파라미터로 Job 실행 시 실패한다")
        void should_fail_with_invalid_year_week_parameter() throws Exception {
            // given
            String invalidYearWeek = "invalid-format";
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearWeek", invalidYearWeek)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("월간 랭킹 배치 Job")
    class 월간_랭킹_배치_Job {

        @Test
        @DisplayName("월간 랭킹 Job이 성공적으로 실행되고 MV 테이블에 데이터가 저장된다")
        void should_execute_monthly_ranking_job_successfully() throws Exception {
            // given
            String yearMonth = "2024-12";
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", yearMonth)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // MV 테이블 검증
            List<MonthlyRankEntity> rankings = monthlyRankRepository.findByYearMonth(yearMonth);
            assertThat(rankings).isNotEmpty();
            assertThat(rankings.size()).isLessThanOrEqualTo(100); // TOP 100
            
            // 순위 검증
            if (!rankings.isEmpty()) {
                assertThat(rankings.get(0).getRankPosition()).isEqualTo(1);
                
                // 점수 순 정렬 검증
                for (int i = 1; i < rankings.size(); i++) {
                    assertThat(rankings.get(i-1).getTotalScore())
                            .isGreaterThanOrEqualTo(rankings.get(i).getTotalScore());
                }
            }
        }

        @Test
        @DisplayName("잘못된 yearMonth 파라미터로 Job 실행 시 실패한다")
        void should_fail_with_invalid_year_month_parameter() throws Exception {
            // given
            String invalidYearMonth = "invalid-format";
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearMonth", invalidYearMonth)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // when
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("멱등성 검증")
    class 멱등성_검증 {

        @Test
        @DisplayName("동일한 파라미터로 Job을 재실행해도 결과가 동일하다")
        void should_produce_same_result_when_job_is_rerun() throws Exception {
            // given
            String yearWeek = "2024-W52";
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("yearWeek", yearWeek)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // when - 첫 번째 실행
            JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
            List<WeeklyRankEntity> firstResult = weeklyRankRepository.findByYearWeek(yearWeek);

            // when - 두 번째 실행 (다른 timestamp로)
            JobParameters secondJobParameters = new JobParametersBuilder()
                    .addString("yearWeek", yearWeek)
                    .addLong("timestamp", System.currentTimeMillis() + 1000)
                    .toJobParameters();
            JobExecution secondExecution = jobLauncherTestUtils.launchJob(secondJobParameters);
            List<WeeklyRankEntity> secondResult = weeklyRankRepository.findByYearWeek(yearWeek);

            // then
            assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // 결과 동일성 검증
            assertThat(firstResult.size()).isEqualTo(secondResult.size());
            
            for (int i = 0; i < firstResult.size(); i++) {
                WeeklyRankEntity first = firstResult.get(i);
                WeeklyRankEntity second = secondResult.get(i);
                
                assertThat(first.getProductId()).isEqualTo(second.getProductId());
                assertThat(first.getRankPosition()).isEqualTo(second.getRankPosition());
                assertThat(first.getTotalScore()).isEqualTo(second.getTotalScore());
            }
        }
    }
}