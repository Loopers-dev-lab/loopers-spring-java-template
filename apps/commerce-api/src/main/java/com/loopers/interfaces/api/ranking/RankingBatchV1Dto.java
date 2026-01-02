package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingAggregationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 랭킹 배치 API DTO
 */
public class RankingBatchV1Dto {
    
    /**
     * 배치 실행 응답
     */
    @Schema(description = "배치 실행 응답")
    public record BatchExecutionResponse(
        @Schema(description = "Job Execution ID")
        Long jobExecutionId,
        @Schema(description = "Job 이름")
        String jobName,
        @Schema(description = "배치 실행 상태 (COMPLETED, FAILED 등)")
        String status,
        @Schema(description = "종료 상태")
        String exitStatus,
        @Schema(description = "집계 기간 (weekly, monthly, both)")
        String period,
        @Schema(description = "메시지")
        String message
    ) {
        public static BatchExecutionResponse from(RankingAggregationInfo info) {
            return new BatchExecutionResponse(
                info.getJobExecutionId(),
                info.getJobName(),
                info.getStatus(),
                info.getExitStatus(),
                info.getPeriod(),
                info.getMessage()
            );
        }
    }
}

