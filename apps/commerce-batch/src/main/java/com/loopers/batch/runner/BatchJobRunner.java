package com.loopers.batch.runner;

import com.loopers.batch.scheduler.RankingBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "batch")
public class BatchJobRunner implements CommandLineRunner {

    private final RankingBatchScheduler rankingBatchScheduler;
    
    private String jobName;

    @Override
    public void run(String... args) throws Exception {
        if (jobName == null || jobName.isEmpty()) {
            log.info("No batch job specified. Application will exit.");
            return;
        }

        log.info("Starting batch job: {}", jobName);
        
        switch (jobName.toLowerCase()) {
            case "weekly-ranking":
                rankingBatchScheduler.runWeeklyRankingJob();
                break;
            case "monthly-ranking":
                rankingBatchScheduler.runMonthlyRankingJob();
                break;
            default:
                log.error("Unknown job name: {}", jobName);
                throw new IllegalArgumentException("Unknown job name: " + jobName);
        }
        
        log.info("Batch job completed: {}", jobName);
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}