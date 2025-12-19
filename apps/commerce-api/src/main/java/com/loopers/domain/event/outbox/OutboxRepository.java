package com.loopers.domain.event.outbox;

import java.util.List;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public interface OutboxRepository {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus);

    /**
     * 재시도 가능한 실패 이벤트 조회
     *
     * @param status        상태 (FAILED)
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능한 이벤트 목록
     */
    List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount);

    OutboxEventEntity save(OutboxEventEntity ready);
}
