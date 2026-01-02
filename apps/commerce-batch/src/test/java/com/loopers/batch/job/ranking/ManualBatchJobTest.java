package com.loopers.batch.job.ranking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 수동 배치 Job 실행 테스트
 * - 실제 데이터가 있을 때 수동으로 실행하여 검증
 * - @Disabled로 기본적으로 비활성화
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
@DisplayName("수동 배치 Job 실행 테스트")
class ManualBatchJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("weeklyRankingJob")
    private Job weeklyRankingJob;

    @Autowired
    @Qualifier("monthlyRankingJob")
    private Job monthlyRankingJob;

    @Test
    @Disabled("수동 실행용 - 실제 데이터가 있을 때만 활성화")
    @DisplayName("주간 랭킹 Job 수동 실행")
    void manual_weekly_ranking_job_execution() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearWeek", "2024-W52")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        jobLauncher.run(weeklyRankingJob, jobParameters);

        // then
        System.out.println("주간 랭킹 Job 실행 완료 - 2024-W52");
        System.out.println("MV 테이블(mv_product_rank_weekly)을 확인하세요.");
    }

    @Test
    @Disabled("수동 실행용 - 실제 데이터가 있을 때만 활성화")
    @DisplayName("월간 랭킹 Job 수동 실행")
    void manual_monthly_ranking_job_execution() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", "2024-12")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        jobLauncher.run(monthlyRankingJob, jobParameters);

        // then
        System.out.println("월간 랭킹 Job 실행 완료 - 2024-12");
        System.out.println("MV 테이블(mv_product_rank_monthly)을 확인하세요.");
    }
}