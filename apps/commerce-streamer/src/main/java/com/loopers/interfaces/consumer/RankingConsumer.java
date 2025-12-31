package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.cache.CacheKeyService;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.messaging.event.KafkaEventMessage;
import com.loopers.support.event.KafkaEventProcessor;
import com.loopers.support.event.OrderPaidPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingConsumer {

    private final KafkaEventProcessor eventProcessor;

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyService cacheKeyService;

    private final ObjectMapper objectMapper;

    private static final double RANKING_VIEW_WEIGHT = 0.1;
    private static final double RANKING_LIKE_WEIGHT = 0.2;
    private static final double RANKING_ORDER_WEIGHT = 0.7;

    private static final Duration RANKING_TTL = Duration.ofDays(2);

    /**
     * 주문 이벤트 구독
     * @param records
     * @param acknowledgment
     */
    @KafkaListener(
            topics = "order-events",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeOrder(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.info("catalog-events 배치 수신. 메시지 수: {}", records.size());

        for (ConsumerRecord<Object, Object> record : records) {
            String eventJson = (String) record.value();
            try {
                eventProcessor.process(this::handle, eventJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // manual ack
        acknowledgment.acknowledge();
    }

    public void handle(KafkaEventMessage<Object> message) {
        String aggregateType = message.getAggregateType(); // ORDER, PRODUCT

        switch (aggregateType) {
            case "ORDER":
                OrderPaidPayload payload = objectMapper.convertValue(
                        message.getPayload(),
                        OrderPaidPayload.class
                );

                for (OrderPaidPayload.OrderItem item : payload.items()) {
                    updateRanking(item.productId(), RANKING_ORDER_WEIGHT * Math.log1p(item.totalPrice().doubleValue()));
                }
                break;
            case "PRODUCT":
                if (message.getEventName().equals("LIKE_CREATED")) {
                    updateRanking(message.getAggregateId(), RANKING_LIKE_WEIGHT);
                } else if (message.getEventName().equals("PRODUCT_VIEWED")) {
                    updateRanking(message.getAggregateId(), RANKING_VIEW_WEIGHT);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 실시간 증분으로 productId의 랭킹 점수 갱신
     * @param productId
     */
    public void updateRanking(Long productId, double score) {
        String key = cacheKeyService.rankingKey(LocalDate.now());

        // Redis zset ZINCRBY
        redisTemplate
                .opsForZSet()
                .incrementScore(key, productId.toString(), score);

//        if (productMetrics.isPresent()) {
//            ProductMetrics pm = productMetrics.get();
//            score = RANKING_VIEW_WEIGHT * pm.getViewCount()
//                    + RANKING_LIKE_WEIGHT * pm.getLikeCount()
//                    + RANKING_ORDER_WEIGHT * Math.log1p(pm.getSalesAmount());
//        }

//        // Redis zset 저장
//        redisTemplate
//                .opsForZSet()
//                .add(key, productId.toString(), score);

        // TTL 2일 설정
        redisTemplate.expire(key, RANKING_TTL);
    }

    /**
     * 좋아요, 상품 상세 조회 이벤트 구독
     * @param records
     * @param acknowledgment
     */
    @KafkaListener(
            topics = "catalog-events",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeCatalog(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.info("catalog-events 배치 수신. 메시지 수: {}", records.size());

        for (ConsumerRecord<Object, Object> record : records) {
            String eventJson = (String) record.value();
            try {
                eventProcessor.process(this::handle, eventJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // manual ack
        acknowledgment.acknowledge();
    }
}
