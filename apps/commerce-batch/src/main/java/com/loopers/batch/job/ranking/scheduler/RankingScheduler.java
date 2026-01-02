package com.loopers.batch.job.ranking.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

  private final JobLauncher jobLauncher;

  @Qualifier("weeklyRankingJob")
  private final Job weeklyRankingJob;

  @Qualifier("monthlyRankingJob")
  private final Job monthlyRankingJob;

  @Scheduled(cron = "0 0 2 * * MON")
  public void runWeeklyRankingJob() {
    String requestDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    log.info(">>> Weekly Ranking Job Scheduler Start: {}", requestDate);

    try {
      jobLauncher.run(weeklyRankingJob, new JobParametersBuilder()
          .addString("requestDate", requestDate)
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters());
    } catch (Exception e) {
      log.error(">>> Weekly Ranking Job Error: {}", e.getMessage());
    }
  }

  @Scheduled(cron = "0 0 3 1 * *")
  public void runMonthlyRankingJob() {
    String requestDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String startDate = LocalDateTime.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    log.info(">>> Monthly Ranking Job Scheduler Start: {}", requestDate);

    try {
      jobLauncher.run(monthlyRankingJob, new JobParametersBuilder()
          .addString("requestDate", requestDate)
          .addString("startDate", startDate)
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters());
    } catch (Exception e) {
      log.error(">>> Monthly Ranking Job Error: {}", e.getMessage());
    }
  }
}
