package com.loopers.application.ranking;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.event.EventType;
import com.loopers.domain.ranking.RankingRepository;
import com.loopers.domain.ranking.RankingScorePolicy;
import com.loopers.infrastructure.ranking.RankingRedisProperties;
import com.loopers.interfaces.consumer.CatalogEventEnvelope;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScoreAccumulator {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final RankingScorePolicy scorePolicy;
  private final RankingRepository rankingRepository;
  private final RankingRedisProperties redisProperties;

  public void accumulate(List<CatalogEventEnvelope> events) {
    if (events == null || events.isEmpty()) {
      return;
    }

    Map<String, Map<Long, Double>> bucketToScores =
        events.stream()
            .filter(event -> calculateScore(event) != 0)
            .collect(
                groupingBy(
                    this::toBucketKey,
                    groupingBy(
                        event -> Long.parseLong(event.aggregateId()),
                        summingDouble(this::calculateScore))));

    if (!bucketToScores.isEmpty()) {
      rankingRepository.incrementScores(bucketToScores);
      log.debug("랭킹 점수 누적 완료: {} 버킷, {} 상품", bucketToScores.size(), countTotalProducts(bucketToScores));
    }
  }

  private double calculateScore(CatalogEventEnvelope event) {
    int quantity = extractQuantityOrDefault(event);
    return scorePolicy.calculateScore(event.eventType(), quantity);
  }

  private int extractQuantityOrDefault(CatalogEventEnvelope event) {
    if (!EventType.PRODUCT_SOLD.matches(event.eventType())) {
      return 1;
    }

    JsonNode payload = event.payload();
    if (payload == null || !payload.has("quantity")) {
      return 1;
    }

    JsonNode quantityNode = payload.get("quantity");
    if (!quantityNode.isInt()) {
      return 1;
    }

    int quantity = quantityNode.asInt();
    return quantity > 0 ? quantity : 1;
  }

  private String toBucketKey(CatalogEventEnvelope event) {
    LocalDate date =
        Instant.ofEpochMilli(event.occurredAt()).atZone(redisProperties.getTimezone()).toLocalDate();
    return redisProperties.getKeyPrefix() + ":" + date.format(DATE_FORMATTER);
  }

  private int countTotalProducts(Map<String, Map<Long, Double>> bucketToScores) {
    return bucketToScores.values().stream().mapToInt(Map::size).sum();
  }
}
