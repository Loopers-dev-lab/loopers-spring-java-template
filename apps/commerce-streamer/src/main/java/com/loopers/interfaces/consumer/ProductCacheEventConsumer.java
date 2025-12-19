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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"product.created.v1", "product.updated.v1", "product.deleted.v1", "product.viewed.v1"},
        groupId = "commerce-streamer-cache-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class ProductCacheEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    @KafkaHandler
    @Transactional
    public void handleCreated(ConsumerRecord<String, ProductEvents.Created> record, Acknowledgment ack) {
        log.info("ProductCacheEventConsumer: ProductEvents.Created 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", event -> {
            // Redis 캐시 Evict (Write-Around 전략)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("ProductCacheEventConsumer: ProductEvents.Updated 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", event -> {
            // Redis 캐시 Evict (Write-Around 전략)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("ProductCacheEventConsumer: ProductEvents.Deleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", event -> {
            // Redis 캐시 Evict (Write-Around 전략)
            productCacheService.evictProductCache(event.productId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleViewed(ConsumerRecord<String, ProductEvents.Viewed> record, Acknowledgment ack) {
        log.info("ProductCacheEventConsumer: ProductEvents.Viewed 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", event -> {
            // 조회 수 집계
            productMetricsService.upsertViewCount(event.productId());
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in product topics: {}", record.value());
        ack.acknowledge();
    }
}

