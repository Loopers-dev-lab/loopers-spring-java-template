package com.loopers.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventRepository;
import com.loopers.domain.common.event.EventStatus;
import com.loopers.messaging.event.KafkaEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final DomainEventRepository domainEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int OUTBOX_PUBLISH_DELAY = 1000;
    private static final int MAX_RETRIES = 5;

    /**
     * 종료시점 기준 1초 마다 도메인이벤트 100개씩 처리하는 스케줄러
     */
    @Scheduled(fixedDelay = OUTBOX_PUBLISH_DELAY)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish() {

        PageRequest pageRequest = PageRequest.of(0, 100,
                Sort.by("createdAt").ascending());

        List<DomainEvent> events =
                domainEventRepository.findByStatus(EventStatus.PENDING, pageRequest);

        for (DomainEvent event : events) {
            try {
                KafkaEventMessage<?> message =
                        objectMapper.readValue(
                                event.getPayload(),
                                KafkaEventMessage.class
                        );

                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId().toString(),
                        message
                ).get();

                event.markSent();
            } catch (Exception e) {
                log.warn("카프카 이벤트 send 중 오류 발생, {}", e.getMessage());

                event.increaseRetryCount();
                if (event.getRetryCount() == MAX_RETRIES) {
                    event.markFailed();
                    log.error("최대 재시도 횟수({}) 초과로 이벤트를 FAILED 처리합니다. eventId={}",
                            MAX_RETRIES, event.getId());
                }
            }
        }
    }
}
