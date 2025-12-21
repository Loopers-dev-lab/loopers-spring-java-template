package com.loopers.interfaces.consumer;

import com.loopers.application.CatalogEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

  private final CatalogEventHandler catalogEventHandler;

  @KafkaListener(
      topics = {"${kafka.topics.catalog-events}"},
      groupId = "${kafka.groups.catalog-events}")
  public void consume(@Payload CatalogEventEnvelope envelope, Acknowledgment acknowledgment) {
    log.debug("메시지 수신: eventId={}, eventType={}", envelope.eventId(), envelope.eventType());

    try {
      boolean processed =
          catalogEventHandler.handle(
              envelope.eventId(),
              envelope.eventType(),
              envelope.aggregateId(),
              envelope.occurredAt(),
              envelope.payload());

      if (processed) {
        acknowledgment.acknowledge();
        log.debug("이벤트 {} 커밋 완료", envelope.eventId());
      }

    } catch (Exception e) {
      log.error("메시지 처리 실패: eventId={}", envelope.eventId(), e);
    }
  }
}