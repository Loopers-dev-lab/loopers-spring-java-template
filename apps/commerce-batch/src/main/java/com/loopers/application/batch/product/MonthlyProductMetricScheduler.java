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
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class MonthlyProductMetricScheduler {

    private final JobLauncher jobLauncher;
    private final Job monthlyProductMetricJob;

    @Scheduled(cron = "0 0 2 1 * ?")
    public void run() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(monthlyProductMetricJob, jobParameters);
    }
}
