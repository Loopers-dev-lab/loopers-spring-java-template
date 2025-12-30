package com.loopers.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingBatchScheduler {

    private final JobLauncher jobLauncher;
    
    @Qualifier("weeklyRankingJob")
    private final Job weeklyRankingJob;
    
    @Qualifier("monthlyRankingJob")
    private final Job monthlyRankingJob;

    public void runWeeklyRankingJob() {
        runWeeklyRankingJob(null);
    }
    
    public void runWeeklyRankingJob(String period) {
        try {
            log.info("Starting weekly ranking batch job for period: {}", period);
            
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis());
            
            if (period != null) {
                builder.addString("period", period);
            }
            
            JobParameters jobParameters = builder.toJobParameters();
            
            jobLauncher.run(weeklyRankingJob, jobParameters);
            log.info("Weekly ranking batch job completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to run weekly ranking batch job", e);
            throw new RuntimeException("Weekly ranking batch job failed", e);
        }
    }

    public void runMonthlyRankingJob() {
        runMonthlyRankingJob(null);
    }
    
    public void runMonthlyRankingJob(String period) {
        try {
            log.info("Starting monthly ranking batch job for period: {}", period);
            
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis());
            
            if (period != null) {
                builder.addString("period", period);
            }
            
            JobParameters jobParameters = builder.toJobParameters();
            
            jobLauncher.run(monthlyRankingJob, jobParameters);
            log.info("Monthly ranking batch job completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to run monthly ranking batch job", e);
            throw new RuntimeException("Monthly ranking batch job failed", e);
        }
    }
}