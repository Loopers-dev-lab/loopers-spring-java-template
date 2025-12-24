package com.loopers.infrastructure.dlq;

import com.loopers.domain.dlq.DlqMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DlqMessageJpaRepository extends JpaRepository<DlqMessage, String> {

    List<DlqMessage> findByStatus(DlqMessage.DlqStatus status);

    @Query("SELECT d FROM DlqMessage d WHERE d.status = :status AND d.retryCount < :maxRetryCount ORDER BY d.createdAt ASC")
    List<DlqMessage> findPendingMessagesForRetry(@Param("status") DlqMessage.DlqStatus status,
                                                 @Param("maxRetryCount") int maxRetryCount,
                                                 Pageable pageable);

    long countByStatus(DlqMessage.DlqStatus status);
}
