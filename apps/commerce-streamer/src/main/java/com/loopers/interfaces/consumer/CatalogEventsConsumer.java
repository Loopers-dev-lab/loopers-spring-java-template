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
import java.util.Map;
import java.util.LinkedHashMap;

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
      log.info("Received {} catalog events", messages.size());

      // 1. 배치 내 동일 eventId 중복 제거 (최신 데이터 우선)
      Map<String, ConsumerRecord<String, String>> latestEventsByEventId = deduplicateByEventId(messages);

      // 2. Inbox에 최신 이벤트만 배치로 저장
      eventHandledService.saveEvents(latestEventsByEventId.values());

      // 3. 즉시 커밋 (메시지 유실 방지)
      acknowledgment.acknowledge();
      log.info("Successfully saved {} unique catalog events to inbox (deduplicated from {})",
          latestEventsByEventId.size(), messages.size());

    } catch (Exception e) {
      log.error("Failed to save catalog events to inbox", e);
      throw e; // 재시도 발생
    }
  }

  /**
   * 배치 내 동일 eventId 중복 제거 (최신 데이터 우선)
   * Kafka offset이 높을수록 최신 데이터로 간주
   */
  private Map<String, ConsumerRecord<String, String>> deduplicateByEventId(List<ConsumerRecord<String, String>> messages) {
    Map<String, ConsumerRecord<String, String>> latestEventsByEventId = new LinkedHashMap<>();

    for (ConsumerRecord<String, String> record : messages) {
      try {
        // Kafka 헤더에서 eventId 추출
        String eventId = extractEventIdFromHeaders(record);

        // 기존 레코드가 없거나, 현재 레코드의 offset이 더 높으면 업데이트
        ConsumerRecord<String, String> existingRecord = latestEventsByEventId.get(eventId);
        if (existingRecord == null || record.offset() > existingRecord.offset()) {
          latestEventsByEventId.put(eventId, record);

          if (existingRecord != null) {
            log.debug("Replaced duplicate eventId={} with newer offset: {} -> {}",
                eventId, existingRecord.offset(), record.offset());
          }
        } else {
          log.debug("Skipped older duplicate eventId={} with offset: {} (keeping {})",
              eventId, record.offset(), existingRecord.offset());
        }

      } catch (Exception e) {
        log.warn("Failed to process message, including it anyway: offset={}", record.offset(), e);
        // 처리 실패시 offset을 키로 사용하여 메시지 유실 방지
        latestEventsByEventId.put("error_" + record.offset(), record);
      }
    }

    return latestEventsByEventId;
  }

  /**
   * Kafka 헤더에서 eventId 추출
   */
  private String extractEventIdFromHeaders(ConsumerRecord<String, String> record) {
    if (record.headers() != null) {
      var eventIdHeader = record.headers().lastHeader("eventId");
      if (eventIdHeader != null && eventIdHeader.value() != null) {
        return new String(eventIdHeader.value());
      }
    }
    log.warn("eventId header not found, using offset as key: offset={}", record.offset());
    return "unknown_" + record.offset();
  }

}
