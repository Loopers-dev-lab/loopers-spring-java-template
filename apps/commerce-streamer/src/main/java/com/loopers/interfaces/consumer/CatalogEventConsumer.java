package com.loopers.interfaces.consumer;

import com.loopers.application.CatalogEventHandler;
import java.util.List;
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
  public void consume(@Payload List<CatalogEventEnvelope> batch, Acknowledgment acknowledgment) {
    log.debug("배치 메시지 수신: size={}", batch.size());

    try {
      catalogEventHandler.handleBatch(batch);
      acknowledgment.acknowledge();
      log.debug("배치 커밋 완료: size={}", batch.size());
    } catch (Exception e) {
      log.error("배치 처리 실패: size={}", batch.size(), e);
    }
  }
}