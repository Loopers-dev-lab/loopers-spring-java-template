package com.loopers.batch.runner;

import com.loopers.batch.scheduler.RankingBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.CommandLineRunner;
import com.loopers.config.BatchJobProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements CommandLineRunner {

  private final RankingBatchScheduler rankingBatchScheduler;
  private final BatchJobProperties properties;

  @Override
  public void run(String... args) {
    String jobKey = properties.getJobName();

    if (jobKey == null || jobKey.isBlank()) {
      log.info("No batch job specified. Application will exit.");
      return;
    }

    log.info("Starting batch job: {}", jobKey);
    JobParametersBuilder builder = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis()); // JobInstance 구분용

    for (String arg : args) {
      if (!arg.startsWith("--") || !arg.contains("=")) {
        continue;
      }

      String[] tokens = arg.substring(2).split("=", 2);
      String key = tokens[0];
      String value = tokens[1];

      // Spring Boot 내부 파라미터 제외
      if (key.equals("job.name")) {
        continue;
      }

      builder.addString(key, value);
    }

    log.info("Batch job completed: {}", builder.toJobParameters().toString());
    rankingBatchScheduler.run(jobKey, builder.toJobParameters());

    log.info("Batch job completed: {}", jobKey);
  }
}

