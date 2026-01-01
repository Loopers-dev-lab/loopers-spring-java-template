package com.loopers.interfaces.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RankingBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job rankingJob;

    public RankingBatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("rankingJob") Job rankingJob
    ) {
        this.jobLauncher = jobLauncher;
        this.rankingJob = rankingJob;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void runWeeklyRankingJob() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String targetDate = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            log.info("주간 랭킹 배치 작업 시작: targetDate={}, time={}", targetDate, LocalDateTime.now());
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("rankingType", "WEEKLY")
                    .addString("targetDate", targetDate)
                    .addString("executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .toJobParameters();
            
            jobLauncher.run(rankingJob, jobParameters);
            
            log.info("주간 랭킹 배치 작업 완료: targetDate={}, time={}", targetDate, LocalDateTime.now());
        } catch (Exception e) {
            log.error("주간 랭킹 배치 작업 실패", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void runMonthlyRankingJob() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String targetDate = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            log.info("월간 랭킹 배치 작업 시작: targetDate={}, time={}", targetDate, LocalDateTime.now());
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("rankingType", "MONTHLY")
                    .addString("targetDate", targetDate)
                    .addString("executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .toJobParameters();
            
            jobLauncher.run(rankingJob, jobParameters);
            
            log.info("월간 랭킹 배치 작업 완료: targetDate={}, time={}", targetDate, LocalDateTime.now());
        } catch (Exception e) {
            log.error("월간 랭킹 배치 작업 실패", e);
        }
    }
}

