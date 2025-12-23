package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.Outbox;
import com.loopers.domain.outbox.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxJpaRepository extends JpaRepository<Outbox, Long> {
    @Query("SELECT o FROM Outbox o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<Outbox> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status, Pageable pageable);

    @Query("SELECT o FROM Outbox o WHERE o.status = :status AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<Outbox> findFailedOutboxesForRetry(@Param("status") OutboxStatus status, @Param("maxRetries") int maxRetries, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Outbox o SET o.status = 'PUBLISHED', o.errorMessage = NULL WHERE o.id = :id")
    void updateOutboxPublishedStatus(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Outbox o SET o.status = 'FAILED', o.errorMessage = :message, o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    void updateOutboxFailedStatusAndErrorMessageAndRetryCount(@Param("id") Long id,
                                                              @Param("message") String message);
}

