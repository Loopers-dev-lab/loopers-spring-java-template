package com.loopers.batch.application;

import com.loopers.batch.domain.ranking.RankingPeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobFacade {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final JobLauncher jobLauncher;
  private final Job rankingAggregationJob;

  public JobExecutionResult runRankingAggregation(RankingPeriod period, LocalDate date) {
    String baseDate = date.format(DATE_FORMATTER);
    log.info("랭킹 집계 Job 실행: period={}, baseDate={}", period, baseDate);

    try {
      JobExecution execution = jobLauncher.run(
          rankingAggregationJob,
          new JobParametersBuilder()
              .addString("period", period.name())
              .addString("baseDate", baseDate)
              .addString("runId", LocalDateTime.now().toString())
              .toJobParameters()
      );

      return new JobExecutionResult(
          execution.getJobId(),
          execution.getStatus().toString(),
          "Job 실행 완료"
      );
    } catch (Exception e) {
      log.error("랭킹 집계 Job 실행 실패", e);
      return new JobExecutionResult(null, "FAILED", e.getMessage());
    }
  }

  public record JobExecutionResult(Long jobId, String status, String message) {}
}
