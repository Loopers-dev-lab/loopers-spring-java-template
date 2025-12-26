package com.loopers.interfaces.consumer;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.application.ranking.RankingEventLogService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 기반 랭킹 배치 이벤트 Consumer
 * Two-Track 전략: DB 로깅(동기) + Redis 반영(비동기)
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
            // 각 이벤트를 개별적으로 처리
            for (ConsumerRecord<String, Object> record : records) {
                try {
                    processRecord(record);
                } catch (Exception e) {
                    log.error("개별 이벤트 처리 실패 - topic: {}, partition: {}, offset: {}, error: {}", 
                            record.topic(), record.partition(), record.offset(), e.getMessage(), e);
                    // 개별 이벤트 실패는 로깅 후 스킵 (DB 로깅이 실패하면 예외가 발생하여 스킵됨)
                }
            }

            // 모든 이벤트 처리 성공 시 커밋
            ack.acknowledge();
            log.info("RankingBatchConsumer: 배치 처리 완료 - {} 개의 이벤트 처리", records.size());

        } catch (Exception e) {
            log.error("배치 처리 실패 - 재시도 예정, error: {}", e.getMessage(), e);
            // 배치 전체 실패 시 재시도를 위해 커밋하지 않음 (Kafka가 자동으로 재시도)
            throw e;
        }
    }

    /**
     * 개별 레코드 처리
     * 1. 멱등성 체크
     * 2. DB 로깅 (동기)
     * 3. Redis 반영 (비동기)
     */
    private void processRecord(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        String topic = record.topic();

        if (value == null) {
            log.warn("Null value in record - topic: {}, partition: {}, offset: {}", 
                    topic, record.partition(), record.offset());
            return;
        }

        // DomainEvent로 변환 시도
        if (!(value instanceof DomainEvent)) {
            log.warn("DomainEvent가 아닌 값 - topic: {}, valueType: {}", topic, value.getClass().getName());
            return;
        }

        DomainEvent event = (DomainEvent) value;
        String eventId = rankingEventLogService.extractEventId(event);

        // 1. 멱등성 체크
        if (rankingEventLogService.isAlreadyProcessed(eventId)) {
            log.debug("이미 처리된 이벤트 - eventId: {}", eventId);
            return;
        }

        // 2. DB 로깅 및 Redis 반영 (토픽별로 분기)
        if ("order.v1".equals(topic)) {
            processOrderEvent(value, eventId, event);
        } else if ("like.v1".equals(topic)) {
            if (value instanceof LikeEvents.ProductLikeSaved) {
                processLikeSaved((LikeEvents.ProductLikeSaved) value, eventId);
            } else if (value instanceof LikeEvents.ProductLikeDeleted) {
                processLikeDeleted((LikeEvents.ProductLikeDeleted) value, eventId);
            }
        } else if ("product.v1".equals(topic) && value instanceof ProductEvents.Viewed) {
            processProductViewed((ProductEvents.Viewed) value, eventId);
        } else {
            log.debug("처리하지 않는 이벤트 타입 - topic: {}, valueType: {}", topic, value.getClass().getName());
        }
    }

    /**
     * 주문 이벤트 처리 (동적 타입 체크)
     * OrderEvents.Created는 commerce-api 모듈에만 존재하므로 리플렉션 사용
     */
    private void processOrderEvent(Object event, String eventId, DomainEvent domainEvent) {
        try {
            // 리플렉션을 사용하여 items() 메서드 호출
            java.lang.reflect.Method itemsMethod = event.getClass().getMethod("items");
            Object items = itemsMethod.invoke(event);
            
            if (items == null) {
                return;
            }
            
            // List로 변환
            @SuppressWarnings("unchecked")
            java.util.List<Object> itemsList = (java.util.List<Object>) items;
            
            if (itemsList.isEmpty()) {
                return;
            }
            
            LocalDateTime occurredAt = domainEvent.getOccurredAt();
            
            for (Object item : itemsList) {
                // OrderItemInfo의 메서드 호출
                java.lang.reflect.Method productIdMethod = item.getClass().getMethod("productId");
                java.lang.reflect.Method priceMethod = item.getClass().getMethod("price");
                java.lang.reflect.Method quantityMethod = item.getClass().getMethod("quantity");
                
                Long productId = (Long) productIdMethod.invoke(item);
                java.math.BigDecimal price = (java.math.BigDecimal) priceMethod.invoke(item);
                Integer quantity = (Integer) quantityMethod.invoke(item);
                
                if (productId == null || price == null || quantity == null) {
                    log.warn("주문 이벤트에 필수 정보 누락 - productId: {}, price: {}, quantity: {}", 
                            productId, price, quantity);
                    continue;
                }
                
                // 점수 계산
                double score = productRankingService.calculateOrderScore(price.doubleValue(), quantity);
                
                // DB 로깅 (동기)
                rankingEventLogService.saveEventLog(
                    eventId, 
                    productId, 
                    RankingEventType.ORDER, 
                    score, 
                    occurredAt
                );
                
                // Redis 반영 (비동기)
                applyScoreToRedisAsync(productId, () -> 
                    productRankingService.incrementOrderScore(productId, price.doubleValue(), quantity)
                );
            }
        } catch (Exception e) {
            log.error("주문 이벤트 처리 실패 - eventType: {}, error: {}", 
                    event.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("주문 이벤트 처리 실패", e);
        }
    }

    /**
     * 좋아요 저장 이벤트 처리
     */
    private void processLikeSaved(LikeEvents.ProductLikeSaved event, String eventId) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        double score = productRankingService.getLikeScore();
        LocalDateTime occurredAt = event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now();

        // DB 로깅 (동기)
        rankingEventLogService.saveEventLog(
            eventId,
            productId,
            RankingEventType.LIKE,
            score,
            occurredAt
        );

        // Redis 반영 (비동기)
        applyScoreToRedisAsync(productId, () -> 
            productRankingService.incrementLikeScore(productId)
        );
    }

    /**
     * 좋아요 삭제 이벤트 처리
     */
    private void processLikeDeleted(LikeEvents.ProductLikeDeleted event, String eventId) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        double score = -productRankingService.getLikeScore(); // 음수로 저장
        LocalDateTime occurredAt = event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now();

        // DB 로깅 (동기)
        rankingEventLogService.saveEventLog(
            eventId,
            productId,
            RankingEventType.LIKE,
            score,
            occurredAt
        );

        // Redis 반영 (비동기)
        applyScoreToRedisAsync(productId, () -> 
            productRankingService.decrementLikeScore(productId)
        );
    }

    /**
     * 상품 조회 이벤트 처리
     */
    private void processProductViewed(ProductEvents.Viewed event, String eventId) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        double score = productRankingService.getViewScore();
        LocalDateTime occurredAt = event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now();

        // DB 로깅 (동기)
        rankingEventLogService.saveEventLog(
            eventId,
            productId,
            RankingEventType.VIEW,
            score,
            occurredAt
        );

        // Redis 반영 (비동기)
        applyScoreToRedisAsync(productId, () -> 
            productRankingService.incrementViewScore(productId)
        );
    }

    /**
     * Redis에 점수를 비동기로 반영
     * 실패해도 DB 로그는 있으므로 스냅샷 집계 시 복구 가능
     */
    private void applyScoreToRedisAsync(Long productId, Runnable applyScore) {
        CompletableFuture.runAsync(() -> {
            try {
                applyScore.run();
            } catch (Exception e) {
                log.error("Redis 반영 실패 (스냅샷에서 복구 예정) - productId: {}, error: {}", 
                        productId, e.getMessage(), e);
                // 실패해도 로깅만 수행하고, 스냅샷 집계 시 자동으로 Redis에 반영됨
            }
        });
    }
}
