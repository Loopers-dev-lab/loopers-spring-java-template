package com.loopers.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.application.strategy.CatalogEventStrategy;
import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import java.time.Clock;
import java.util.List;
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
  private final Clock clock;

  @Transactional
  public boolean handle(
      String eventId, String eventType, String aggregateId, Long occurredAt, JsonNode payload) {
    // 멱등성 보장: 먼저 INSERT 시도, 중복이면 예외로 처리
    try {
      eventHandledRepository.save(EventHandled.of(eventId, clock.instant()));
    } catch (DataIntegrityViolationException e) {
      log.debug("이벤트 {} 이미 처리됨, 스킵", eventId);
      return true;
    }

    Long productId = Long.parseLong(aggregateId);

    // Strategy 패턴으로 이벤트 타입별 처리
    strategies.stream()
        .filter(strategy -> strategy.supports(eventType))
        .findFirst()
        .ifPresentOrElse(
            strategy -> strategy.handle(productId, occurredAt, payload),
            () -> log.debug("알 수 없는 이벤트 타입: {}, 스킵", eventType));

    return true;
  }
}