package com.loopers.infrastructure.event.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * 재시도 가능한 실패 이벤트 조회
     *
     * @param status        상태 (FAILED)
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능한 이벤트 목록 (최대 50개)
     */
    List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount);
}
