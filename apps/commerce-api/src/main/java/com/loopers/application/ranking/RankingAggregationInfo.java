package com.loopers.application.ranking;

import lombok.Getter;
import org.springframework.batch.core.JobExecution;

/**
 * 랭킹 집계 배치 실행 결과 정보
 */
@Getter
public class RankingAggregationInfo {

    private final Long jobExecutionId;
    private final String jobName;
    private final String status;
    private final String exitStatus;
    private final String period; // "weekly" or "monthly"
    private final String message;

    private RankingAggregationInfo(
            Long jobExecutionId,
            String jobName,
            String status,
            String exitStatus,
            String period,
            String message
    ) {
        this.jobExecutionId = jobExecutionId;
        this.jobName = jobName;
        this.status = status;
        this.exitStatus = exitStatus;
        this.period = period;
        this.message = message;
    }

    public static RankingAggregationInfo from(JobExecution jobExecution, String period) {
        return new RankingAggregationInfo(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus().name(),
                jobExecution.getExitStatus() != null ? jobExecution.getExitStatus().getExitCode() : null,
                period,
                jobExecution.getExitStatus() != null ? jobExecution.getExitStatus().getExitDescription() : null
        );
    }

    public static RankingAggregationInfo combined(
            RankingAggregationInfo weekly,
            RankingAggregationInfo monthly
    ) {
        boolean bothSuccess = "COMPLETED".equals(weekly.status) && "COMPLETED".equals(monthly.status);
        String combinedStatus = bothSuccess ? "COMPLETED" : "FAILED";
        String combinedMessage = String.format(
                "Weekly: %s, Monthly: %s",
                weekly.status,
                monthly.status
        );

        return new RankingAggregationInfo(
                null, // combined에는 jobExecutionId 없음
                "weeklyAndMonthlyRankingJob",
                combinedStatus,
                bothSuccess ? "COMPLETED" : "FAILED",
                "both",
                combinedMessage
        );
    }
}

