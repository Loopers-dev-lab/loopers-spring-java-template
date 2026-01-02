package com.loopers.infrastructure.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.ranking.RankingScoreCalculator;
import com.loopers.domain.ranking.RankingService;
import com.loopers.infrastructure.dlq.DlqService;
import com.loopers.infrastructure.idempotency.IdempotencyService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 랭킹 관련 Kafka 이벤트 리스너
 * <p>
 * catalog-events와 order-events 토픽에서 이벤트를 소비하여
 * Redis ZSET에 랭킹 점수를 적재합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final IdempotencyService idempotencyService;
    private final RankingService rankingService;
    private final RankingScoreCalculator scoreCalculator;
    private final OrderRepository orderRepository;
    private final DlqService dlqService;
    private final ObjectMapper objectMapper;

    // 메시지별 재시도 횟수 추적 (메모리 기반, 재시작 시 초기화됨)
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * catalog-events 토픽에서 랭킹 관련 이벤트 처리
     * - ProductViewed: 조회 점수 추가
     * - ProductLiked: 좋아요 점수 추가
     * - ProductUnliked: 좋아요 점수 차감 (음수)
     */
    @KafkaListener(
            topics = "catalog-events",
            groupId = "commerce-ranking-consumer-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    @Transactional
    public void handleCatalogEventsForRanking(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received {} messages from catalog-events for ranking", records.size());

        String todayKey = rankingService.getTodayRankingKey();

        for (ConsumerRecord<Object, Object> record : records) {
            try {
                Map<String, Object> message = parseMessage(record.value());
                String eventId = extractEventId(message);
                String eventType = extractEventType(message);
                String aggregateId = record.key() != null ?
                        record.key().toString() : extractAggregateId(message);

                if (eventId == null || eventType == null || aggregateId == null) {
                    log.warn("Missing required fields: eventId={}, eventType={}, aggregateId={}",
                            eventId, eventType, aggregateId);
                    continue;
                }

                // 멱등성 체크 (랭킹 처리용 별도 ID 사용)
                String rankingEventId = eventId + ":ranking";
                if (idempotencyService.isAlreadyHandled(rankingEventId)) {
                    log.debug("Event already handled for ranking: eventId={}", eventId);
                    continue;
                }

                // 랭킹 점수 계산 및 적재
                Long productId = Long.parseLong(aggregateId);
                double score = scoreCalculator.calculateScore(eventType, message);

                if (score != 0.0) {
                    // ProductUnliked의 경우 음수 점수로 차감
                    if ("ProductUnliked".equals(eventType)) {
                        score = -scoreCalculator.calculateLikeScore();
                    }

                    Double newScore = rankingService.incrementScore(todayKey, productId, score);
                    log.debug("Updated ranking score: productId={}, score={}, newTotal={}",
                            productId, score, newScore);
                }

                // 멱등성 기록 (랭킹 처리용 별도 기록)
                idempotencyService.markAsHandled(
                        rankingEventId,
                        eventType,
                        aggregateId,
                        "RankingEventListener"
                );

            } catch (Exception e) {
                String messageKey = record.key() != null ? record.key().toString() :
                        extractEventId(parseMessage(record.value()));
                int retryCount = retryCountMap.getOrDefault(messageKey, 0) + 1;
                retryCountMap.put(messageKey, retryCount);

                log.error("Failed to process ranking event: topic={}, key={}, offset={}, retryCount={}",
                        record.topic(), record.key(), record.offset(), retryCount, e);

                // 재시도 횟수 초과 시 DLQ로 전송
                if (retryCount >= MAX_RETRY_COUNT) {
                    try {
                        dlqService.sendToDlq(
                                record.topic(),
                                record.key(),
                                record.value(),
                                e.getMessage(),
                                retryCount
                        );
                        retryCountMap.remove(messageKey);
                        log.warn("Message sent to DLQ after {} retries: topic={}, key={}",
                                retryCount, record.topic(), record.key());
                        continue;
                    } catch (Exception dlqException) {
                        log.error("Failed to send message to DLQ: topic={}, key={}",
                                record.topic(), record.key(), dlqException);
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        acknowledgment.acknowledge();
        log.debug("Acknowledged {} ranking messages from catalog-events", records.size());
    }

    /**
     * order-events 토픽에서 주문 완료 이벤트 처리
     * - OrderPaid: 주문 점수 추가 (각 주문 항목별로 처리)
     */
    @KafkaListener(
            topics = "order-events",
            groupId = "commerce-ranking-consumer-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    @Transactional
    public void handleOrderEventsForRanking(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received {} messages from order-events for ranking", records.size());

        String todayKey = rankingService.getTodayRankingKey();

        for (ConsumerRecord<Object, Object> record : records) {
            try {
                Map<String, Object> message = parseMessage(record.value());
                String eventId = extractEventId(message);
                String eventType = extractEventType(message);

                if (!"OrderPaid".equals(eventType)) {
                    continue; // 주문 완료 이벤트만 처리
                }

                // 멱등성 체크
                String rankingEventId = eventId + ":ranking";
                if (idempotencyService.isAlreadyHandled(rankingEventId)) {
                    log.debug("Order event already handled for ranking: eventId={}", eventId);
                    continue;
                }

                // orderId 추출
                Object orderIdObj = message.get("orderId");
                if (orderIdObj == null) {
                    log.warn("OrderPaid event missing orderId: {}", message);
                    continue;
                }

                Long orderId;
                try {
                    orderId = Long.parseLong(orderIdObj.toString());
                } catch (NumberFormatException e) {
                    log.error("Failed to parse orderId: {}", orderIdObj, e);
                    continue;
                }

                // Order 엔티티 조회하여 주문 항목 가져오기
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                                "주문을 찾을 수 없습니다: orderId=" + orderId));

                // 각 주문 항목에 대해 랭킹 점수 추가
                order.getOrderItems().forEach(item -> {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();
                    Long price = Long.valueOf(item.getPricePerItem().amount());

                    if (productId == null || quantity == null || quantity <= 0 || price == null || price <= 0) {
                        log.warn("Invalid OrderItem: productId={}, quantity={}, price={}",
                                productId, quantity, price);
                        return;
                    }

                    try {
                        double score = scoreCalculator.calculateOrderScore(price, quantity);
                        Double newScore = rankingService.incrementScore(todayKey, productId, score);
                        log.debug("Updated ranking score from order: productId={}, score={}, newTotal={}",
                                productId, score, newScore);
                    } catch (Exception e) {
                        log.error("Failed to update ranking score: productId={}, quantity={}, price={}",
                                productId, quantity, price, e);
                        throw e; // 트랜잭션 롤백을 위해 예외 재발생
                    }
                });

                // 멱등성 기록
                idempotencyService.markAsHandled(
                        rankingEventId,
                        eventType,
                        record.key() != null ? record.key().toString() : orderId.toString(),
                        "RankingEventListener"
                );

            } catch (Exception e) {
                String messageKey = record.key() != null ? record.key().toString() :
                        extractEventId(parseMessage(record.value()));
                int retryCount = retryCountMap.getOrDefault(messageKey, 0) + 1;
                retryCountMap.put(messageKey, retryCount);

                log.error("Failed to process order ranking event: topic={}, key={}, offset={}, retryCount={}",
                        record.topic(), record.key(), record.offset(), retryCount, e);

                // 재시도 횟수 초과 시 DLQ로 전송
                if (retryCount >= MAX_RETRY_COUNT) {
                    try {
                        dlqService.sendToDlq(
                                record.topic(),
                                record.key(),
                                record.value(),
                                e.getMessage(),
                                retryCount
                        );
                        retryCountMap.remove(messageKey);
                        log.warn("Message sent to DLQ after {} retries: topic={}, key={}",
                                retryCount, record.topic(), record.key());
                        continue;
                    } catch (Exception dlqException) {
                        log.error("Failed to send message to DLQ: topic={}, key={}",
                                record.topic(), record.key(), dlqException);
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        acknowledgment.acknowledge();
        log.debug("Acknowledged {} ranking messages from order-events", records.size());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value instanceof String) {
            try {
                return objectMapper.readValue((String) value, Map.class);
            } catch (Exception e) {
                log.error("Failed to parse message as JSON", e);
                throw new RuntimeException("Failed to parse message", e);
            }
        }
        throw new IllegalArgumentException("Unsupported message type: " + value.getClass());
    }

    private String extractEventId(Map<String, Object> message) {
        if (message.containsKey("eventId")) {
            return message.get("eventId").toString();
        }
        if (message.containsKey("id")) {
            return message.get("id").toString();
        }
        return null;
    }

    private String extractEventType(Map<String, Object> message) {
        if (message.containsKey("eventType")) {
            return message.get("eventType").toString();
        }
        if (message.containsKey("type")) {
            return message.get("type").toString();
        }
        return null;
    }

    private String extractAggregateId(Map<String, Object> message) {
        if (message.containsKey("aggregateId")) {
            return message.get("aggregateId").toString();
        }
        if (message.containsKey("productId")) {
            return message.get("productId").toString();
        }
        return null;
    }
}

