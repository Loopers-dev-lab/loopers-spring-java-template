package com.loopers.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingBatchScheduler {

  private final JobLauncher jobLauncher;
  private final Map<String, Job> jobs;

  public void run(String jobKey, JobParameters params) {
    Job job = jobs.get(jobKey);

    if (job == null) {
      throw new IllegalArgumentException("No such job: " + jobKey);
    }

    try {
      jobLauncher.run(job, params);
    } catch (Exception e) {
      throw new RuntimeException("Failed to run job: " + jobKey, e);
    }
  }
}

