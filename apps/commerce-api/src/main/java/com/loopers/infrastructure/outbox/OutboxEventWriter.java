package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.ImmediatePublishEvent;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxEventUpdater outboxEventUpdater;
  private final KafkaTemplate<Object, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final OutboxProperties outboxProperties;

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void handle(DomainEvent event) {
    String topic = event.eventType().getTopic();
    if (topic == null) {
      return;
    }

    String payload = extractPayload(event);
    long occurredAtMillis =
        event.occurredAt().atZone(clock.getZone()).toInstant().toEpochMilli();

    OutboxEvent outboxEvent =
        OutboxEvent.create(
            event.eventId(),
            topic,
            event.eventType().getCode(),
            event.aggregateId(),
            payload,
            occurredAtMillis);

    outboxEventRepository.save(outboxEvent);

    log.debug(
        "Outbox 이벤트 생성: eventId={}, topic={}, type={}, aggregateId={}",
        event.eventId(),
        topic,
        event.eventType().getCode(),
        event.aggregateId());
  }
  
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishImmediately(ImmediatePublishEvent event) {
    String eventId = event.eventId();
    Instant now = clock.instant();
    Instant leaseExpiry = now.plus(outboxProperties.getLeaseDuration());

    // 선점 시도 (NEW → SENDING)
    int updated = outboxEventUpdater.updateStatusToSending(eventId, leaseExpiry);
    if (updated == 0) {
      log.debug("이벤트 {} 선점 실패, Relay가 처리 예정", eventId);
      return;
    }

    try {
      OutboxEvent outboxEvent = outboxEventUpdater.findById(eventId)
          .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Outbox 이벤트 없음: " + eventId));

      KafkaEnvelope envelope = buildEnvelope(outboxEvent);
      kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), envelope)
          .whenComplete((result, ex) -> {
            if (ex != null) {
              log.warn("즉시 발행 실패, Relay가 재시도 예정: eventId={}, error={}", eventId, ex.getMessage());
              outboxEventUpdater.resetToNew(eventId);
            } else {
              outboxEventUpdater.toSent(outboxEvent);
              log.debug("즉시 발행 성공: eventId={}, topic={}", eventId, outboxEvent.getTopic());
            }
          });
    } catch (Exception e) {
      log.warn("즉시 발행 중 오류, Relay가 재시도 예정: eventId={}, error={}", eventId, e.getMessage());
      outboxEventUpdater.resetToNew(eventId);
    }
  }

  private KafkaEnvelope buildEnvelope(OutboxEvent event) throws JsonProcessingException {
    return KafkaEnvelope.of(
        event.getEventId(),
        event.getEventType(),
        event.getAggregateId(),
        event.getOccurredAt(),
        objectMapper.readTree(event.getPayload()));
  }

  private String extractPayload(DomainEvent event) {
    try {
      return objectMapper.writeValueAsString(event.payload());
    } catch (Exception e) {
      log.warn("payload 직렬화 실패, 빈 객체로 대체: eventId={}", event.eventId(), e);
      return "{}";
    }
  }
}
