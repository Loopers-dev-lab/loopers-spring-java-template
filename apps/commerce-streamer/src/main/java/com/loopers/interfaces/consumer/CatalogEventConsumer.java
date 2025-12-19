package com.loopers.interfaces.consumer;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"product.updated.v1", "product.deleted.v1", "product.like-count-changed.v1"},
        groupId = "commerce-streamer-catalog-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class CatalogEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    @KafkaHandler
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("Received ProductEvents.Updated: {}", record.value().productId());
        messageProcessor.execute(record, ack, "catalog", event -> 
            productCacheService.evictProductCache(event.productId())
        );
    }

    @KafkaHandler
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("Received ProductEvents.Deleted: {}", record.value().productId());
        messageProcessor.execute(record, ack, "catalog", event -> 
            productCacheService.evictProductCache(event.productId())
        );
    }

    @KafkaHandler
    public void handleLikeCount(ConsumerRecord<String, ProductEvents.LikeCount> record, Acknowledgment ack) {
        log.info("Received ProductEvents.LikeCount: {} (delta: {})", record.value().productId(), record.value().delta());
        messageProcessor.execute(record, ack, "catalog", event -> 
            productMetricsService.upsertLikeCount(event.productId(), event.delta())
        );
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in product topics: {}", record.value());
        ack.acknowledge();
    }
}
