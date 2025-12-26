package com.loopers.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * PENDING 상태의 이벤트를 생성 시각 순으로 조회
     * @param limit 조회 개수
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
        @Param("status") OutboxEvent.OutboxStatus status,
        @Param("limit") int limit
    );

    /**
     * 재시도 가능한 FAILED 이벤트 조회
     * @param maxRetryCount 최대 재시도 횟수
     * @param limit 조회 개수
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' AND o.retryCount < :maxRetryCount ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findRetryableEvents(
        @Param("maxRetryCount") int maxRetryCount,
        @Param("limit") int limit
    );

    /**
     * 특정 파티션 키의 이벤트 조회 (디버깅용)
     */
    List<OutboxEvent> findByPartitionKeyOrderByCreatedAtDesc(String partitionKey);

    /**
     * 특정 이벤트 타입 조회 (디버깅용)
     */
    List<OutboxEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * 오래된 PUBLISHED 이벤트 삭제
     * @param status 이벤트 상태 (PUBLISHED)
     * @param publishedBefore 발행 완료 시각 기준
     * @return 삭제된 이벤트 개수
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = :status AND o.publishedAt < :publishedBefore")
    int deletePublishedEventsOlderThan(
        @Param("status") OutboxEvent.OutboxStatus status,
        @Param("publishedBefore") ZonedDateTime publishedBefore
    );

    /**
     * 오래된 FAILED 이벤트 삭제 (재시도 횟수 초과)
     * @param status 이벤트 상태 (FAILED)
     * @param createdBefore 생성 시각 기준
     * @param maxRetryCount 최대 재시도 횟수
     * @return 삭제된 이벤트 개수
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = :status AND o.createdAt < :createdBefore AND o.retryCount >= :maxRetryCount")
    int deleteFailedEventsOlderThan(
        @Param("status") OutboxEvent.OutboxStatus status,
        @Param("createdBefore") ZonedDateTime createdBefore,
        @Param("maxRetryCount") int maxRetryCount
    );

    /**
     * 상태별 이벤트 개수 조회
     * @param status 이벤트 상태
     * @return 이벤트 개수
     */
    long countByStatus(OutboxEvent.OutboxStatus status);
}
