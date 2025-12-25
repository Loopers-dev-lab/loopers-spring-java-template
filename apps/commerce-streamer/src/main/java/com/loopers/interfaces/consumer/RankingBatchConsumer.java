package com.loopers.interfaces.consumer;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.event.ProductEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka 기반 랭킹 배치 이벤트 Consumer
 * 배치로 이벤트를 수신하여 상품별 점수를 합산한 후 Redis에 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
public class RankingBatchConsumer {

    private final ProductRankingService productRankingService;

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
            // 상품별 점수 합산을 위한 Map
            Map<Long, ScoreAccumulator> scoreMap = new HashMap<>();

            // 이벤트 타입별로 분류하여 처리
            for (ConsumerRecord<String, Object> record : records) {
                try {
                    processRecord(record, scoreMap);
                } catch (Exception e) {
                    log.error("개별 이벤트 처리 실패 - topic: {}, partition: {}, offset: {}, error: {}", 
                            record.topic(), record.partition(), record.offset(), e.getMessage(), e);
                    // 개별 이벤트 실패는 로깅 후 스킵
                }
            }

            // 배치 처리 완료 후 Redis에 한 번에 반영
            applyScoresToRedis(scoreMap);

            // 모든 이벤트 처리 성공 시 커밋
            ack.acknowledge();
            log.info("RankingBatchConsumer: 배치 처리 완료 - {} 개의 상품 점수 업데이트", scoreMap.size());

        } catch (Exception e) {
            log.error("배치 처리 실패 - 재시도 예정, error: {}", e.getMessage(), e);
            // 배치 전체 실패 시 재시도를 위해 커밋하지 않음 (Kafka가 자동으로 재시도)
            throw e;
        }
    }

    /**
     * 개별 레코드 처리
     */
    private void processRecord(ConsumerRecord<String, Object> record, Map<Long, ScoreAccumulator> scoreMap) {
        Object value = record.value();
        String topic = record.topic();

        if (value == null) {
            log.warn("Null value in record - topic: {}, partition: {}, offset: {}", 
                    topic, record.partition(), record.offset());
            return;
        }

        // 토픽별로 이벤트 타입 분기
        // OrderEvents는 commerce-api 모듈에만 존재하므로 동적 타입 체크
        if ("order.v1".equals(topic)) {
            processOrderEvent(value, scoreMap);
        } else if ("like.v1".equals(topic)) {
            if (value instanceof LikeEvents.ProductLikeSaved) {
                processLikeSaved((LikeEvents.ProductLikeSaved) value, scoreMap);
            } else if (value instanceof LikeEvents.ProductLikeDeleted) {
                processLikeDeleted((LikeEvents.ProductLikeDeleted) value, scoreMap);
            }
        } else if ("product.v1".equals(topic) && value instanceof ProductEvents.Viewed) {
            processProductViewed((ProductEvents.Viewed) value, scoreMap);
        } else {
            log.debug("처리하지 않는 이벤트 타입 - topic: {}, valueType: {}", topic, value.getClass().getName());
        }
    }

    /**
     * 주문 이벤트 처리 (동적 타입 체크)
     * OrderEvents.Created는 commerce-api 모듈에만 존재하므로 리플렉션 사용
     */
    private void processOrderEvent(Object event, Map<Long, ScoreAccumulator> scoreMap) {
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
                
                ScoreAccumulator accumulator = scoreMap.computeIfAbsent(productId, k -> new ScoreAccumulator());
                accumulator.addOrder(price.doubleValue(), quantity);
            }
        } catch (Exception e) {
            log.error("주문 이벤트 처리 실패 - eventType: {}, error: {}", 
                    event.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * 좋아요 저장 이벤트 처리
     */
    private void processLikeSaved(LikeEvents.ProductLikeSaved event, Map<Long, ScoreAccumulator> scoreMap) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        ScoreAccumulator accumulator = scoreMap.computeIfAbsent(productId, k -> new ScoreAccumulator());
        accumulator.addLike();
    }

    /**
     * 좋아요 삭제 이벤트 처리
     */
    private void processLikeDeleted(LikeEvents.ProductLikeDeleted event, Map<Long, ScoreAccumulator> scoreMap) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        ScoreAccumulator accumulator = scoreMap.computeIfAbsent(productId, k -> new ScoreAccumulator());
        accumulator.removeLike();
    }

    /**
     * 상품 조회 이벤트 처리
     */
    private void processProductViewed(ProductEvents.Viewed event, Map<Long, ScoreAccumulator> scoreMap) {
        Long productId = event.productId();
        if (productId == null) {
            return;
        }

        ScoreAccumulator accumulator = scoreMap.computeIfAbsent(productId, k -> new ScoreAccumulator());
        accumulator.addView();
    }

    /**
     * 합산된 점수를 Redis에 반영
     */
    private void applyScoresToRedis(Map<Long, ScoreAccumulator> scoreMap) {
        for (Map.Entry<Long, ScoreAccumulator> entry : scoreMap.entrySet()) {
            Long productId = entry.getKey();
            ScoreAccumulator accumulator = entry.getValue();

            try {
                // 주문 점수 적용
                for (OrderScore orderScore : accumulator.orderScores) {
                    productRankingService.incrementOrderScore(
                            productId, 
                            orderScore.price, 
                            orderScore.quantity
                    );
                }

                // 좋아요 점수 적용
                if (accumulator.likeDelta != 0) {
                    if (accumulator.likeDelta > 0) {
                        for (int i = 0; i < accumulator.likeDelta; i++) {
                            productRankingService.incrementLikeScore(productId);
                        }
                    } else {
                        for (int i = 0; i < -accumulator.likeDelta; i++) {
                            productRankingService.decrementLikeScore(productId);
                        }
                    }
                }

                // 조회수 점수 적용
                for (int i = 0; i < accumulator.viewCount; i++) {
                    productRankingService.incrementViewScore(productId);
                }

            } catch (Exception e) {
                log.error("Redis 점수 반영 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
                // 개별 상품 실패는 로깅 후 계속 진행
            }
        }
    }

    /**
     * 점수 누적을 위한 내부 클래스
     */
    private static class ScoreAccumulator {
        private final List<OrderScore> orderScores = new java.util.ArrayList<>();
        private int likeDelta = 0;
        private int viewCount = 0;

        void addOrder(double price, int quantity) {
            orderScores.add(new OrderScore(price, quantity));
        }

        void addLike() {
            likeDelta++;
        }

        void removeLike() {
            likeDelta--;
        }

        void addView() {
            viewCount++;
        }
    }

    /**
     * 주문 점수 정보
     */
    private record OrderScore(double price, int quantity) {}
}

