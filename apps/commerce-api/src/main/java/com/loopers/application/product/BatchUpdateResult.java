package com.loopers.application.product;

import java.util.Collections;
import java.util.Set;

import lombok.Getter;

/**
 * 배치 업데이트 결과 DTO
 * 변경된 상품/브랜드 ID를 포함하여 캐시 무효화에 사용
 */
@Getter
public class BatchUpdateResult {

    private final boolean success;
    private final int createdCount;
    private final int updatedCount;
    private final long durationMs;
    private final String errorMessage;

    // 변경된 상품 ID 목록 (생성 또는 갱신된 상품)
    private final Set<Long> changedProductIds;

    // 변경된 상품들이 속한 브랜드 ID 목록
    private final Set<Long> affectedBrandIds;

    private BatchUpdateResult(
            boolean success,
            int createdCount,
            int updatedCount,
            long durationMs,
            String errorMessage,
            Set<Long> changedProductIds,
            Set<Long> affectedBrandIds
    ) {
        this.success = success;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.changedProductIds = changedProductIds != null
                ? Set.copyOf(changedProductIds)
                : Collections.emptySet();
        this.affectedBrandIds = affectedBrandIds != null
                ? Set.copyOf(affectedBrandIds)
                : Collections.emptySet();
    }

    /**
     * 성공 결과 생성
     */
    public static BatchUpdateResult success(
            int createdCount,
            int updatedCount,
            long durationMs,
            Set<Long> changedProductIds,
            Set<Long> affectedBrandIds
    ) {
        return new BatchUpdateResult(
                true,
                createdCount,
                updatedCount,
                durationMs,
                null,
                changedProductIds,
                affectedBrandIds
        );
    }

    /**
     * 성공 결과 생성 (변경 추적 없음)
     */
    public static BatchUpdateResult success(int createdCount, int updatedCount, long durationMs) {
        return success(createdCount, updatedCount, durationMs, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * 실패 결과 생성
     */
    public static BatchUpdateResult failure(String errorMessage, long durationMs) {
        return new BatchUpdateResult(
                false,
                0,
                0,
                durationMs,
                errorMessage,
                Collections.emptySet(),
                Collections.emptySet()
        );
    }

    /**
     * 변경 사항 있는지 확인
     */
    public boolean hasChanges() {
        return success && (createdCount > 0 || updatedCount > 0);
    }

    /**
     * 전체 처리 건수 (생성 + 갱신)
     */
    public int getTotalCount() {
        return createdCount + updatedCount;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format(
                    "BatchUpdateResult{success=true, created=%d, updated=%d, duration=%dms}",
                    createdCount, updatedCount, durationMs
            );
        } else {
            return String.format(
                    "BatchUpdateResult{success=false, error='%s', duration=%dms}",
                    errorMessage, durationMs
            );
        }
    }
}
