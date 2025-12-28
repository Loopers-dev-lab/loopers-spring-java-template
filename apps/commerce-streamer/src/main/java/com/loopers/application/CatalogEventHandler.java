package com.loopers.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.application.ranking.RankingScoreAccumulator;
import com.loopers.application.strategy.CatalogEventStrategy;
import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.interfaces.consumer.CatalogEventEnvelope;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogEventHandler {

  private final EventHandledRepository eventHandledRepository;
  private final List<CatalogEventStrategy> strategies;
  private final RankingScoreAccumulator rankingScoreAccumulator;
  private final Clock clock;

  @Transactional
  public boolean handle(
      String eventId, String eventType, String aggregateId, Long occurredAt, JsonNode payload) {
    try {
      eventHandledRepository.save(EventHandled.of(eventId, clock.instant()));
    } catch (DataIntegrityViolationException e) {
      log.debug("이벤트 {} 이미 처리됨, 스킵", eventId);
      return true;
    }

    Long productId = Long.parseLong(aggregateId);

    strategies.stream()
        .filter(strategy -> strategy.supports(eventType))
        .findFirst()
        .ifPresentOrElse(
            strategy -> strategy.handle(productId, occurredAt, payload),
            () -> log.debug("알 수 없는 이벤트 타입: {}, 스킵", eventType));

    return true;
  }

  @Transactional
  public void handleBatch(List<CatalogEventEnvelope> batch) {
    if (batch == null || batch.isEmpty()) {
      return;
    }

    List<String> allEventIds = batch.stream().map(CatalogEventEnvelope::eventId).toList();
    Set<String> existingEventIds = eventHandledRepository.findExistingEventIds(allEventIds);

    List<CatalogEventEnvelope> newEvents =
        batch.stream()
            .filter(event -> !existingEventIds.contains(event.eventId()))
            .toList();

    if (newEvents.isEmpty()) {
      log.debug("배치 내 모든 이벤트가 이미 처리됨, 스킵");
      return;
    }

    for (CatalogEventEnvelope event : newEvents) {
      Long productId = Long.parseLong(event.aggregateId());
      strategies.stream()
          .filter(strategy -> strategy.supports(event.eventType()))
          .findFirst()
          .ifPresentOrElse(
              strategy -> strategy.handle(productId, event.occurredAt(), event.payload()),
              () -> log.debug("알 수 없는 이벤트 타입: {}, 스킵", event.eventType()));
    }

    rankingScoreAccumulator.accumulate(newEvents);

    List<EventHandled> handledEvents =
        newEvents.stream()
            .map(event -> EventHandled.of(event.eventId(), clock.instant()))
            .toList();
    eventHandledRepository.saveAll(handledEvents);

    log.debug("배치 처리 완료: 전체={}, 신규={}", batch.size(), newEvents.size());
  }
}