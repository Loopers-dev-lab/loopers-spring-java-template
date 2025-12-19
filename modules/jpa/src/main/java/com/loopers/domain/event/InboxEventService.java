package com.loopers.domain.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxEventService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public void process(String messageId, LocalDateTime eventTimestamp, Runnable action) {
        Optional<InboxEvent> existingEventOptional = inboxEventRepository.findByMessageId(messageId);

        if (existingEventOptional.isPresent()) {
            InboxEvent existingEvent = existingEventOptional.get();
            if (existingEvent.getEventTimestamp().isAfter(eventTimestamp)) {
                log.info("Older event received (messageId: {}). Skipping processing.", messageId);
                // 더 최신 이벤트가 이미 처리되었으므로 현재 이벤트 무시
                return; // 함수 종료
            } else {
                log.info("Event already handled or same timestamp (messageId: {}). Skipping processing.", messageId);
                // 이미 처리되었거나 같은 타임스탬프 이벤트이므로 무시
                return; // 함수 종료
            }
        }

        try {
            action.run();
            inboxEventRepository.save(InboxEvent.builder()
                    .messageId(messageId)
                    .eventTimestamp(eventTimestamp)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Event already handled (race condition): {}", messageId);
            // 이미 다른 스레드/인스턴스에서 처리. 무시.
        }
    }
}

