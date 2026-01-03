package com.loopers.interfaces.api.batch;

import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchJobController {
    
    private final JobLauncher jobLauncher;
    private final Job weeklyRankingJob;
    private final Job monthlyRankingJob;
    
    /**
     * 주간 랭킹 집계 Job 실행
     */
    @PostMapping("/weekly-ranking")
    public ApiResponse<Object> runWeeklyRankingJob(
        @RequestParam(value = "targetDate", required = false) String targetDate
    ) {
        try {
            String dateParam = targetDate != null ? targetDate : LocalDate.now().toString();
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", dateParam)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(weeklyRankingJob, jobParameters);
            
            log.info("주간 랭킹 집계 Job 실행 완료 - targetDate: {}", dateParam);
            return ApiResponse.success("주간 랭킹 집계 Job이 성공적으로 실행되었습니다.");
            
        } catch (Exception e) {
            log.error("주간 랭킹 집계 Job 실행 실패", e);
            return ApiResponse.fail("BATCH_ERROR", "주간 랭킹 집계 Job 실행에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 월간 랭킹 집계 Job 실행
     */
    @PostMapping("/monthly-ranking") 
    public ApiResponse<Object> runMonthlyRankingJob(
        @RequestParam(value = "targetDate", required = false) String targetDate
    ) {
        try {
            String dateParam = targetDate != null ? targetDate : LocalDate.now().toString();
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", dateParam)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(monthlyRankingJob, jobParameters);
            
            log.info("월간 랭킹 집계 Job 실행 완료 - targetDate: {}", dateParam);
            return ApiResponse.success("월간 랭킹 집계 Job이 성공적으로 실행되었습니다.");
            
        } catch (Exception e) {
            log.error("월간 랭킹 집계 Job 실행 실패", e);
            return ApiResponse.fail("BATCH_ERROR", "월간 랭킹 집계 Job 실행에 실패했습니다: " + e.getMessage());
        }
    }
}