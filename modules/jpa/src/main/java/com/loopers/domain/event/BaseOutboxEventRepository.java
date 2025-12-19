package com.loopers.domain.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 도메인별 Outbox Event Repository의 공통 쿼리를 정의하는 인터페이스
 * @NoRepositoryBean을 사용하여 Spring Data JPA가 이 인터페이스 자체를 Repository Bean으로 만들지 않도록 함
 */
@NoRepositoryBean
public interface BaseOutboxEventRepository<T extends BaseOutboxEvent> extends JpaRepository<T, Long> {

    /**
     * 발행 대기 중인 이벤트를 조회합니다.
     */
    @Query("SELECT o FROM #{#entityName} o WHERE o.status = 'PENDING' AND o.createdAt <= :beforeTime ORDER BY o.createdAt ASC")
    List<T> findAllPendingEventsBefore(@Param("beforeTime") LocalDateTime beforeTime, Pageable pageable);

    /**
     * 발행 대기 중이거나 재시도 가능한 이벤트를 조회합니다.
     * - PENDING 상태이거나
     * - FAILED 상태이면서 재시도 가능하고 재시도 시간이 지난 경우
     */
    @Query("SELECT o FROM #{#entityName} o WHERE " +
           "(o.status = 'PENDING' AND o.createdAt <= :beforeTime) OR " +
           "(o.status = 'FAILED' AND o.retryCount < o.maxRetries AND o.nextRetryAt <= :now) " +
           "ORDER BY o.createdAt ASC")
    List<T> findPendingEventsForProcessing(
            @Param("beforeTime") LocalDateTime beforeTime,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    /**
     * 배치로 상태를 업데이트합니다.
     */
    @Modifying
    @Query("UPDATE #{#entityName} o SET o.status = :status, o.publishedAt = :publishedAt WHERE o.id IN :ids")
    int updateStatusBatch(
            @Param("ids") List<Long> ids,
            @Param("status") OutboxStatus status,
            @Param("publishedAt") LocalDateTime publishedAt
    );
}

