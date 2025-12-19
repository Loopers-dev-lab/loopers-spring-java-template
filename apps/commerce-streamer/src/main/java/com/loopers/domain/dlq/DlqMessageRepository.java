package com.loopers.domain.dlq;

import java.util.List;
import java.util.Optional;

public interface DlqMessageRepository {

    DlqMessage save(DlqMessage dlqMessage);

    Optional<DlqMessage> findById(String id);

    List<DlqMessage> findByStatus(DlqMessage.DlqStatus status);

    List<DlqMessage> findPendingMessagesForRetry(int maxRetryCount, int limit);

    List<DlqMessage> findAll();

    long countByStatus(DlqMessage.DlqStatus status);
}
