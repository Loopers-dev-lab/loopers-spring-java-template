package com.loopers.interfaces.consumer;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"like.product-saved.v1", "like.product-deleted.v1", "like.count-changed.v1"},
        groupId = "commerce-streamer-like-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class LikeEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    @KafkaHandler
    @Transactional
    public void handleProductLikeSaved(ConsumerRecord<String, LikeEvents.ProductLikeSaved> record, Acknowledgment ack) {
        log.info("LikeEventConsumer: ProductLikeSaved 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "like", event -> {
            // 좋아요 수 증가 (ProductMetricsService를 통해)
            productMetricsService.upsertLikeCount(event.productId(), 1L);
            // Redis 캐시 업데이트 (Write-Through)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleProductLikeDeleted(ConsumerRecord<String, LikeEvents.ProductLikeDeleted> record, Acknowledgment ack) {
        log.info("LikeEventConsumer: ProductLikeDeleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "like", event -> {
            // 좋아요 수 감소 (ProductMetricsService를 통해)
            productMetricsService.upsertLikeCount(event.productId(), -1L);
            // Redis 캐시 업데이트 (Write-Through)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleLikeCountChanged(ConsumerRecord<String, LikeEvents.LikeCountChanged> record, Acknowledgment ack) {
        log.info("LikeEventConsumer: LikeCountChanged 수신 - productId: {}, delta: {}", 
                record.value().productId(), record.value().delta());

        messageProcessor.execute(record, ack, "like", event -> {
            // ProductMetricsService를 통해 좋아요 수 집계
            productMetricsService.upsertLikeCount(event.productId(), event.delta());
            // Redis 캐시 업데이트 (Write-Through)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in like topics: {}", record.value());
        ack.acknowledge();
    }
}

