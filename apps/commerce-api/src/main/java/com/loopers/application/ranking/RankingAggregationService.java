package com.loopers.application.ranking;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 랭킹 집계 배치 서비스
 * <p>
 * 주간/월간 랭킹 집계 배치를 실행하는 서비스를 제공합니다.
 */
@Slf4j
@Component
public class RankingAggregationService {

    private final JobLauncher jobLauncher;
    private final Job rankingJob;

    public RankingAggregationService(
            JobLauncher jobLauncher,
            @Qualifier("rankingJob") Job rankingJob
    ) {
        this.jobLauncher = jobLauncher;
        this.rankingJob = rankingJob;
    }

    /**
     * 주간 랭킹 집계 배치 실행
     *
     * @param targetDate 집계 대상 날짜 (이 날짜가 속한 주간을 집계)
     * @return JobExecution 실행 결과
     * @note Spring Batch는 자체 트랜잭션을 관리하므로 @Transactional을 사용하지 않습니다.
     */
    public RankingAggregationInfo executeWeeklyRanking(ZonedDateTime targetDate) {
        if (targetDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "집계 대상 날짜는 필수입니다.");
        }

        log.info("주간 랭킹 집계 배치 실행 시작: targetDate={}", targetDate);

        try {
            // 주간 시작일/종료일 계산
            ZonedDateTime weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay(targetDate.getZone());
            ZonedDateTime weekEnd = targetDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .toLocalDate()
                    .atTime(23, 59, 59)
                    .atZone(targetDate.getZone());
            ZonedDateTime rankingDate = weekEnd;

            // Job 파라미터 생성
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("periodType", "weekly")
                    .addString("rankingDate", rankingDate.toString())
                    .addString("startDate", weekStart.toLocalDate().toString())
                    .addString("endDate", weekEnd.toLocalDate().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // 배치 실행
            JobExecution jobExecution = jobLauncher.run(rankingJob, jobParameters);

            log.info("주간 랭킹 집계 배치 실행 완료: jobExecutionId={}, status={}, exitStatus={}",
                    jobExecution.getId(),
                    jobExecution.getStatus(),
                    jobExecution.getExitStatus());

            return RankingAggregationInfo.from(jobExecution, "weekly");

        } catch (Exception e) {
            log.error("주간 랭킹 집계 배치 실행 실패: targetDate={}", targetDate, e);
            throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "주간 랭킹 집계 배치 실행에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 월간 랭킹 집계 배치 실행
     *
     * @param targetDate 집계 대상 날짜 (이 날짜가 속한 월간을 집계)
     * @return JobExecution 실행 결과
     * @note Spring Batch는 자체 트랜잭션을 관리하므로 @Transactional을 사용하지 않습니다.
     */
    public RankingAggregationInfo executeMonthlyRanking(ZonedDateTime targetDate) {
        if (targetDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "집계 대상 날짜는 필수입니다.");
        }

        log.info("월간 랭킹 집계 배치 실행 시작: targetDate={}", targetDate);

        try {
            // 월간 시작일/종료일 계산
            ZonedDateTime monthStart = targetDate.with(TemporalAdjusters.firstDayOfMonth())
                    .toLocalDate()
                    .atStartOfDay(targetDate.getZone());
            ZonedDateTime monthEnd = targetDate.with(TemporalAdjusters.lastDayOfMonth())
                    .toLocalDate()
                    .atTime(23, 59, 59)
                    .atZone(targetDate.getZone());
            ZonedDateTime rankingDate = monthEnd;

            // Job 파라미터 생성
            // monthly는 Reader에서 최근 30일로 자동 계산하므로 startDate/endDate 생략
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("periodType", "monthly")
                    .addString("rankingDate", rankingDate.toString())
                    // startDate, endDate는 Reader에서 자동 계산 (최근 30일)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // 배치 실행
            JobExecution jobExecution = jobLauncher.run(rankingJob, jobParameters);

            log.info("월간 랭킹 집계 배치 실행 완료: jobExecutionId={}, status={}, exitStatus={}",
                    jobExecution.getId(),
                    jobExecution.getStatus(),
                    jobExecution.getExitStatus());

            return RankingAggregationInfo.from(jobExecution, "monthly");

        } catch (Exception e) {
            log.error("월간 랭킹 집계 배치 실행 실패: targetDate={}", targetDate, e);
            throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "월간 랭킹 집계 배치 실행에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 주간 및 월간 랭킹 집계 배치 실행
     *
     * @param targetDate 집계 대상 날짜
     * @return 배치 실행 결과
     * @note Spring Batch는 자체 트랜잭션을 관리하므로 @Transactional을 사용하지 않습니다.
     */
    public RankingAggregationInfo executeWeeklyAndMonthlyRanking(ZonedDateTime targetDate) {
        if (targetDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "집계 대상 날짜는 필수입니다.");
        }

        log.info("주간 및 월간 랭킹 집계 배치 실행 시작: targetDate={}", targetDate);

        try {
            // 주간 랭킹 집계
            RankingAggregationInfo weeklyResult = executeWeeklyRanking(targetDate);

            // 월간 랭킹 집계
            RankingAggregationInfo monthlyResult = executeMonthlyRanking(targetDate);

            log.info("주간 및 월간 랭킹 집계 배치 실행 완료");

            return RankingAggregationInfo.combined(weeklyResult, monthlyResult);

        } catch (Exception e) {
            log.error("주간 및 월간 랭킹 집계 배치 실행 실패: targetDate={}", targetDate, e);
            throw new CoreException(ErrorType.INTERNAL_ERROR,
                    "주간 및 월간 랭킹 집계 배치 실행에 실패했습니다: " + e.getMessage());
        }
    }
}

