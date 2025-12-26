package com.loopers.domain.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 도메인 이벤트를 Outbox 테이블에 저장
     * 주의: 이 메서드는 반드시 도메인 트랜잭션 내에서 호출되어야 함
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object eventPayload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved: eventType={}, aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "Json 직렬화 실패");
        }
    }
}

