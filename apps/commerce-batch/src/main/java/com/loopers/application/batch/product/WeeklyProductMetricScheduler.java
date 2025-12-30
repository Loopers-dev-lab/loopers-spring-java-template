package com.loopers.application.batch.product;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

@Component
@RequiredArgsConstructor
public class WeeklyProductMetricScheduler {

    private final JobLauncher jobLauncher;
    private final Job weeklyProductMetricJob;

    @Scheduled(cron = "0 0 2 ? * MON")
    public void run() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        LocalDate now = LocalDate.now();
        LocalDate lastMonday = now.minusDays(1).with(ChronoField.DAY_OF_WEEK, 1);
        LocalDate lastSunday = now.minusDays(1);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", lastMonday.toString())
                .addString("endDate", lastSunday.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(weeklyProductMetricJob, jobParameters);
    }
}
