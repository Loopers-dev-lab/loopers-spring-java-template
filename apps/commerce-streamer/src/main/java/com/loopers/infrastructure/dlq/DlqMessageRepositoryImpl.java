package com.loopers.infrastructure.dlq;

import com.loopers.domain.dlq.DlqMessage;
import com.loopers.domain.dlq.DlqMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DlqMessageRepositoryImpl implements DlqMessageRepository {

    private final DlqMessageJpaRepository dlqMessageJpaRepository;

    @Override
    public DlqMessage save(DlqMessage dlqMessage) {
        return dlqMessageJpaRepository.save(dlqMessage);
    }

    @Override
    public Optional<DlqMessage> findById(String id) {
        return dlqMessageJpaRepository.findById(id);
    }

    @Override
    public List<DlqMessage> findByStatus(DlqMessage.DlqStatus status) {
        return dlqMessageJpaRepository.findByStatus(status);
    }

    @Override
    public List<DlqMessage> findPendingMessagesForRetry(int maxRetryCount, int limit) {
        return dlqMessageJpaRepository.findPendingMessagesForRetry(DlqMessage.DlqStatus.PENDING, maxRetryCount, PageRequest.of(0, 100));
    }

    @Override
    public List<DlqMessage> findAll() {
        return dlqMessageJpaRepository.findAll();
    }

    @Override
    public long countByStatus(DlqMessage.DlqStatus status) {
        return dlqMessageJpaRepository.countByStatus(status);
    }
}
