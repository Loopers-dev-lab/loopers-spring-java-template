package com.loopers.infrastructure.event.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * 지정된 개수만큼 상태별 이벤트 조회 (배치 크기 조절 가능)
     */
    @Query(value = "SELECT * FROM outbox_event WHERE status = :status ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<OutboxEventEntity> findTopNByStatusOrderByCreatedAtAsc(
            @Param("status") String status, @Param("limit") int limit);

    /**
     * 재시도 가능한 실패 이벤트 조회
     *
     * @param status        상태 (FAILED)
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 가능한 이벤트 목록 (최대 50개)
     */
    List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount);

    /**
     * 지정된 개수만큼 재시도 가능한 실패 이벤트 조회 (배치 크기 조절 가능)
     */
    @Query(value = "SELECT * FROM outbox_event WHERE status = :status AND retry_count < :maxRetryCount ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<OutboxEventEntity> findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            @Param("status") String status, @Param("maxRetryCount") int maxRetryCount, @Param("limit") int limit);
}
