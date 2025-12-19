package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OutboxProperties.class)
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

  private final OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<Object, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final OutboxProperties properties;

  @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:200}")
  @Transactional
  public void relay() {
    Instant now = clock.instant();

    int recovered = outboxEventRepository.recoverExpiredEvents(now);
    if (recovered > 0) {
      log.info("만료된 이벤트 {}건 복구", recovered);
    }

    List<OutboxEvent> events =
        outboxEventRepository.findNewEventsReadyToSend(now, properties.getBatchSize());

    for (OutboxEvent event : events) {
      processEvent(event, now);
    }
  }

  private void processEvent(OutboxEvent event, Instant now) {
    Instant leaseExpiry = now.plus(properties.getLeaseDuration());
    int updated = outboxEventRepository.updateStatusToSending(event.getEventId(), leaseExpiry);

    if (updated == 0) {
      log.debug("이벤트 {} 이미 다른 프로세스에서 전송 중", event.getEventId());
      return;
    }

    try {
      KafkaEnvelope envelope = buildEnvelope(event);
      sendToKafka(event, envelope);
    } catch (Exception e) {
      handleFailure(event, e, now);
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

  private void sendToKafka(OutboxEvent event, KafkaEnvelope envelope) {
    CompletableFuture<SendResult<Object, Object>> future =
        kafkaTemplate.send(event.getTopic(), event.getAggregateId(), envelope);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            handleFailure(event, ex, clock.instant());
          } else {
            handleSuccess(event, result);
          }
        });
  }

  private void handleSuccess(OutboxEvent event, SendResult<Object, Object> result) {
    event.toSent();
    outboxEventRepository.save(event);

    log.debug(
        "이벤트 {} 전송 성공: topic={}, partition={}, offset={}",
        event.getEventId(),
        result.getRecordMetadata().topic(),
        result.getRecordMetadata().partition(),
        result.getRecordMetadata().offset());
  }

  private void handleFailure(OutboxEvent event, Throwable error, Instant now) {
    String errorMessage = error.getMessage();
    log.warn("이벤트 {} 전송 실패: {}", event.getEventId(), errorMessage);

    List<Duration> backoff = properties.getRetryBackoff();
    int retryIndex = Math.min(event.getRetryCount(), backoff.size() - 1);
    Instant nextRetry = now.plus(backoff.get(retryIndex));

    boolean scheduled = event.scheduleRetry(properties.getMaxRetry(), errorMessage, nextRetry);
    if (!scheduled) {
      event.toDead();
      log.error("이벤트 {} 최대 재시도 초과, DEAD 처리", event.getEventId());
    }

    outboxEventRepository.save(event);
  }
}
