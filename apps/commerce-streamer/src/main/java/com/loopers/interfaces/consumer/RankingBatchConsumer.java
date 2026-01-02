package com.loopers.interfaces.consumer;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.application.ranking.RankingEventLogService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.shared.event.DomainEvent;
import com.loopers.shared.event.LikeEvents;
import com.loopers.shared.event.OrderEvents;
import com.loopers.shared.event.ProductEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kafka 기반 랭킹 배치 이벤트 Consumer
 * DB 로깅 전략: 이벤트를 DB에 저장하고, Redis는 스케줄러가 주기적으로 재구성함
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
public class RankingBatchConsumer {

    private final ProductRankingService productRankingService;
    private final RankingEventLogService rankingEventLogService;

    /**
     * 배치로 수신한 이벤트들을 처리
     * Spring Kafka는 List<ConsumerRecord>를 받는 메서드를 자동으로 배치 리스너로 인식
     * 
     * 배치 처리 파이프라인:
     * 1. Extract IDs -> 2. Batch Check Processed -> 3. Filter -> 4. Create Event Logs -> 5. Batch Save Logs to DB
     * 
     * Redis 업데이트는 스케줄러가 주기적으로 재구성함 (1분: hourly, 10분: daily)
     */
    @KafkaListener(
            topics = {"order.v1", "like.v1", "product.v1"},
            groupId = "commerce-streamer-ranking-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleBatch(List<ConsumerRecord<String, Object>> records, Acknowledgment ack) {
        if (records == null || records.isEmpty()) {
            ack.acknowledge();
            return;
        }

        log.info("RankingBatchConsumer: 배치 수신 - {} 개의 이벤트", records.size());

        try {
            // 1. Extract Event IDs
            List<String> eventIds = records.stream()
                    .map(record -> {
                        Object value = record.value();
                        if (value instanceof DomainEvent event) {
                            return rankingEventLogService.extractEventId(event);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            // 2. Batch Check Processed (멱등성 체크)
            Set<String> processedEventIds = rankingEventLogService.getProcessedEventIds(eventIds);
            log.debug("이미 처리된 이벤트: {} 개", processedEventIds.size());

            // 3. Filter & Parse Events
            List<EventData> eventDataList = new ArrayList<>();
            for (ConsumerRecord<String, Object> record : records) {
                try {
                    EventData eventData = parseEvent(record);
                    if (eventData != null && !processedEventIds.contains(eventData.eventId())) {
                        eventDataList.add(eventData);
                    }
                } catch (Exception e) {
                    log.warn("이벤트 파싱 실패 - topic: {}, partition: {}, offset: {}, error: {}", 
                            record.topic(), record.partition(), record.offset(), e.getMessage());
                }
            }

            if (eventDataList.isEmpty()) {
                log.debug("처리할 이벤트가 없음 (모두 이미 처리됨)");
                ack.acknowledge();
                return;
            }

            // 4. Create Event Logs for DB Storage
            List<RankingEventLog> eventLogs = new ArrayList<>();

            for (EventData eventData : eventDataList) {
                // 각 ProductScore에 대해 로그 생성 (ORDER의 경우 각 아이템마다 로그 필요)
                for (ProductScore productScore : eventData.productScores()) {
                    RankingEventLog eventLog = createEventLog(eventData.eventId(), eventData.occurredAt(), productScore);
                    if (eventLog != null) {
                        eventLogs.add(eventLog);
                    }
                }
            }

            // 5. Batch Save Logs to DB (동기 처리)
            // Redis 업데이트는 스케줄러가 주기적으로 재구성함 (1분: hourly, 10분: daily)
            if (!eventLogs.isEmpty()) {
                rankingEventLogService.saveEventLogs(eventLogs);
                log.debug("배치 저장 완료 - {} 개의 이벤트 로그", eventLogs.size());
            }

            // 모든 이벤트 처리 성공 시 커밋
            // Redis는 스케줄러가 주기적으로 DB에서 집계하여 재구성함
            ack.acknowledge();
            log.info("RankingBatchConsumer: 배치 처리 완료 - {} 개의 이벤트 처리 (신규: {}, 중복: {})", 
                    records.size(), eventDataList.size(), processedEventIds.size());

        } catch (Exception e) {
            log.error("배치 처리 실패 - 재시도 예정, error: {}", e.getMessage(), e);
            // 배치 전체 실패 시 재시도를 위해 커밋하지 않음 (Kafka가 자동으로 재시도)
            throw e;
        }
    }

    /**
     * 이벤트를 파싱하여 EventData로 변환
     */
    private EventData parseEvent(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        String topic = record.topic();

        if (value == null) {
            log.warn("Null value in record - topic: {}, partition: {}, offset: {}", 
                    topic, record.partition(), record.offset());
            return null;
        }

        if (!(value instanceof DomainEvent event)) {
            log.warn("DomainEvent가 아닌 값 - topic: {}, valueType: {}", topic, value.getClass().getName());
            return null;
        }

        String eventId = rankingEventLogService.extractEventId(event);
        if (eventId == null || eventId.isBlank()) {
            log.warn("eventId가 없음 - topic: {}", topic);
            return null;
        }

        LocalDateTime occurredAt = event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now();
        List<ProductScore> productScores = new ArrayList<>();

        // 토픽별로 이벤트 파싱
        if ("order.v1".equals(topic) && value instanceof OrderEvents.Created orderCreated) {
        List<OrderEvents.OrderItemInfo> items = orderCreated.items();
            if (items != null && !items.isEmpty()) {
        for (OrderEvents.OrderItemInfo item : items) {
            Long productId = item.productId();
                    BigDecimal price = item.price();
            Integer quantity = item.quantity();
            
                    if (productId != null && price != null && quantity != null) {
                        double score = productRankingService.calculateOrderScore(price.doubleValue(), quantity);
                        productScores.add(new ProductScore(productId, score, RankingEventType.ORDER, price, quantity));
                    }
                }
            }
        } else if ("like.v1".equals(topic)) {
            Long productId = null;
            double score = 0.0;
            
            if (value instanceof LikeEvents.ProductLikeSaved likeSaved) {
                productId = likeSaved.productId();
                score = productRankingService.getLikeScore();
            } else if (value instanceof LikeEvents.ProductLikeDeleted likeDeleted) {
                productId = likeDeleted.productId();
                score = -productRankingService.getLikeScore(); // 음수
            }
            
            if (productId != null) {
                productScores.add(new ProductScore(productId, score, RankingEventType.LIKE, null, null));
            }
        } else if ("product.v1".equals(topic) && value instanceof ProductEvents.Viewed viewed) {
            Long productId = viewed.productId();
            if (productId != null) {
                double score = productRankingService.getViewScore();
                productScores.add(new ProductScore(productId, score, RankingEventType.VIEW, null, null));
            }
        }

        if (productScores.isEmpty()) {
            return null;
        }

        return new EventData(eventId, occurredAt, productScores);
    }

    /**
     * ProductScore로부터 RankingEventLog 생성
     */
    private RankingEventLog createEventLog(String eventId, LocalDateTime occurredAt, ProductScore productScore) {
        if (productScore.eventType() == RankingEventType.ORDER && productScore.rawPrice() != null && productScore.rawQuantity() != null) {
            return RankingEventLog.builder()
                    .eventId(eventId)
                    .productId(productScore.productId())
                    .eventType(productScore.eventType())
                    .score(productScore.score())
                    .occurredAt(occurredAt)
                    .rawPrice(productScore.rawPrice())
                    .rawQuantity(productScore.rawQuantity())
                    .build();
        } else {
            return RankingEventLog.builder()
                    .eventId(eventId)
                    .productId(productScore.productId())
                    .eventType(productScore.eventType())
                    .score(productScore.score())
                    .occurredAt(occurredAt)
                    .build();
        }
    }

    /**
     * 이벤트 데이터를 담는 내부 클래스
     */
    private record EventData(
            String eventId,
            LocalDateTime occurredAt,
            List<ProductScore> productScores
    ) {}

    /**
     * 상품별 점수 정보를 담는 내부 클래스
     */
    private record ProductScore(
            Long productId,
            Double score,
            RankingEventType eventType,
            BigDecimal rawPrice,  // ORDER 이벤트의 경우
            Integer rawQuantity   // ORDER 이벤트의 경우
    ) {}

}
