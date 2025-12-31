package com.loopers.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@Component
public class JobListener {

    @BeforeJob
    void beforeJob(JobExecution jobExecution) {
        log.info("Job '${jobExecution.jobInstance.jobName}' 시작");
        jobExecution.getExecutionContext().putLong("startTime", System.currentTimeMillis());
    }

    @AfterJob
    void afterJob(JobExecution jobExecution) {
        var startTime = jobExecution.getExecutionContext().getLong("startTime");
        var endTime = System.currentTimeMillis();

        var startDateTime = Instant.ofEpochMilli(startTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        var endDateTime = Instant.ofEpochMilli(endTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        var totalTime = endTime - startTime;
        var duration = Duration.ofMillis(totalTime);
        var hours = duration.toHours();
        var minutes = duration.toMinutes() % 60;
        var seconds = duration.getSeconds() % 60;

        var message = String.format(
            """
                *Start Time:* %s
                *End Time:* %s
                *Total Time:* %d시간 %d분 %d초
            """, startDateTime, endDateTime, hours, minutes, seconds
        ).trim();

        log.info(message);
    }
}
