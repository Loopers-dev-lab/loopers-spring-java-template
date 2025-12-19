package com.loopers.domain.event.outbox;

import java.util.List;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public interface OutboxRepository {
    /**
     * READY 상태 이벤트 조회
     */
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus);

    /**
     * 지정된 개수만큼 상태별 이벤트 조회 (배치 크기 조절 가능)
     */
    List<OutboxEventEntity> findTopNByStatusOrderByCreatedAtAsc(OutboxStatus status, int limit);

    /**
     * 재시도 가능한 실패 이벤트 조회
     *
     * @param status        상태 (FAILED)
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능한 이벤트 목록
     */
    List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount);

    /**
     * 지정된 개수만큼 재시도 가능한 실패 이벤트 조회 (배치 크기 조절 가능)
     */
    List<OutboxEventEntity> findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount, int limit);

    OutboxEventEntity save(OutboxEventEntity ready);
    
    /**
     * 여러 이벤트를 일괄 저장 (원자성 보장)
     */
    List<OutboxEventEntity> saveAll(List<OutboxEventEntity> events);
}
