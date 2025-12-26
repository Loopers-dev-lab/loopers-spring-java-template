package com.loopers.infrastructure.persistence;

import com.loopers.domain.dlq.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DLQ Message Repository
 */
public interface DlqMessageRepository extends JpaRepository<DlqMessage, Long> {

    /**
     * 미해결 DLQ 메시지 조회
     */
    List<DlqMessage> findByResolvedFalseOrderByFailedAtDesc();

    /**
     * 특정 토픽의 DLQ 메시지 조회
     */
    List<DlqMessage> findByOriginalTopicOrderByFailedAtDesc(String originalTopic);

    /**
     * 특정 기간 이전의 해결된 DLQ 메시지 삭제 (정리용)
     */
    @Query("DELETE FROM DlqMessage d WHERE d.resolved = true AND d.failedAt < :cutoffDate")
    int deleteResolvedMessagesOlderThan(ZonedDateTime cutoffDate);
}
