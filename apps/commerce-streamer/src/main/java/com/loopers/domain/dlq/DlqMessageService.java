package com.loopers.domain.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqMessageService {

    private final DlqMessageRepository dlqMessageRepository;

    private static final int MAX_RETRY_COUNT = 5;

    @Transactional
    public DlqMessage saveDlqMessage(
            String originalTopic,
            Integer partitionNum,
            Long offsetNum,
            String messageKey,
            String payload,
            String errorMessage
    ) {
        DlqMessage dlqMessage = DlqMessage.create(
                originalTopic,
                partitionNum,
                offsetNum,
                messageKey,
                payload,
                errorMessage
        );
        DlqMessage saved = dlqMessageRepository.save(dlqMessage);
        log.info("DLQ 메시지 저장: id={}, originalTopic={}", saved.getId(), originalTopic);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DlqMessage> getPendingMessages() {
        return dlqMessageRepository.findByStatus(DlqMessage.DlqStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<DlqMessage> getMessagesForRetry(int limit) {
        return dlqMessageRepository.findPendingMessagesForRetry(MAX_RETRY_COUNT, limit);
    }

    @Transactional
    public void markAsResolved(String id) {
        dlqMessageRepository.findById(id).ifPresent(message -> {
            message.markAsResolved();
            dlqMessageRepository.save(message);
            log.info("DLQ 메시지 해결 완료: id={}", id);
        });
    }

    @Transactional
    public void markAsAbandoned(String id) {
        dlqMessageRepository.findById(id).ifPresent(message -> {
            message.markAsAbandoned();
            dlqMessageRepository.save(message);
            log.warn("DLQ 메시지 포기 처리: id={}", id);
        });
    }

    @Transactional
    public void incrementRetryCount(String id) {
        dlqMessageRepository.findById(id).ifPresent(message -> {
            message.incrementRetryCount();
            dlqMessageRepository.save(message);
        });
    }

    @Transactional(readOnly = true)
    public long countPendingMessages() {
        return dlqMessageRepository.countByStatus(DlqMessage.DlqStatus.PENDING);
    }
}
