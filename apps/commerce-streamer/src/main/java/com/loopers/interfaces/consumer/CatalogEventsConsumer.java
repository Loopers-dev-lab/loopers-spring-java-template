package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.loopers.domain.event.EventHandledService;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventsConsumer {

  private final EventHandledService eventHandledService;

  @KafkaListener(
      topics = {"catalog-events"},
      containerFactory = KafkaConfig.BATCH_LISTENER
  )
  public void handleCatalogEvents(
      List<ConsumerRecord<String, String>> messages,
      Acknowledgment acknowledgment
  ) throws JsonProcessingException {
    try {
      log.info("Received {} order events", messages.size());

      // 1. Inbox에 이벤트 저장 (중복 방지)
      for (ConsumerRecord<String, String> record : messages) {
        eventHandledService.saveEvent(record);
      }

      // 2. 즉시 커밋 (메시지 유실 방지)
      acknowledgment.acknowledge();
      log.info("Successfully saved {} order events to inbox", messages.size());

    } catch (Exception e) {
      log.error("Failed to save order events to inbox", e);
      throw e; // 재시도 발생
    }
  }


}
