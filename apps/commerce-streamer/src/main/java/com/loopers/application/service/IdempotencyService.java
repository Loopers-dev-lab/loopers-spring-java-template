package com.loopers.application.service;

import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

  private final EventHandledRepository eventHandledRepository;

  /**
   * 이벤트가 이미 처리되었는지 확인
   */
  @Transactional(readOnly = true)
  public boolean isEventAlreadyHandled(String businessKey) {
    return eventHandledRepository.existsByBusinessKey(businessKey);
  }

  /**
   * 이벤트 처리 완료 기록
   */
  @Transactional
  public void markEventAsHandled(ConsumerRecord<String, String> record, String businessKey, String eventType) {
    try {
      EventHandled eventHandled = new EventHandled(
          businessKey,
          eventType,
          record.topic(),
          record.value()
      );
      eventHandledRepository.save(eventHandled);
      log.info("Event marked as handled: businessKey={}, topic={}, partition={}, offset={}",
          businessKey, record.topic(), record.partition(), record.offset());
    } catch (Exception e) {
      log.error("Failed to mark event as handled: businessKey={}", businessKey, e);
      throw e;
    }
  }

  /**
   * 멱등성을 보장하며 이벤트 처리
   */
  @Transactional
  public boolean processbusinessKeyempotently(ConsumerRecord<String, String> record, String businessKey, String eventType, Runnable eventProcessor) {
    // 이미 처리된 이벤트인지 확인
    if (isEventAlreadyHandled(businessKey)) {
      log.info("Event already handled, skipping: businessKey={}, topic={}, partition={}, offset={}",
          businessKey, record.topic(), record.partition(), record.offset());
      return false;
    }

    try {
      // 비즈니스 로직 실행
      eventProcessor.run();

      // 처리 완료 기록
      markEventAsHandled(record, businessKey, eventType);

      log.info("Event processed successfully: businessKey={}, eventType={}", businessKey, eventType);
      return true;
    } catch (Exception e) {
      log.error("Failed to process event: businessKey={}, eventType={}", businessKey, eventType, e);
      throw e;
    }
  }


}
