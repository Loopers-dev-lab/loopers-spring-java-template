package com.loopers.interfaces.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @PostMapping("/weekly-ranking")
    public ResponseEntity<String> runWeeklyRankingJob(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 방지
                .toJobParameters();

        Job job = jobRegistry.getJob("weeklyRankingJob");
        jobLauncher.run(job, params);

        return ResponseEntity.ok("WeeklyRankingJob started!");
    }

    @PostMapping("/monthly-ranking")
    public ResponseEntity<String> runMonthlyRankingJob(
            @RequestParam String yearMonth
    ) throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addString("yearMonth", yearMonth)
                .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 방지
                .toJobParameters();

        Job job = jobRegistry.getJob("monthlyRankingJob");
        JobExecution execution = jobLauncher.run(job, params);

        if (execution.getStatus() == BatchStatus.FAILED) {
            return ResponseEntity.status(500).body("monthlyRankingJob failed!");
        }

        return ResponseEntity.ok("monthlyRankingJob started!");
    }
}
