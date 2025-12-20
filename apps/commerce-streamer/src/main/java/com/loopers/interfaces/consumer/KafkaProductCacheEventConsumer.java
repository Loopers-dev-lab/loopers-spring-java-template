package com.loopers.interfaces.consumer;

import com.loopers.domain.product.event.ProductCacheEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 Product 캐시 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 ProductCacheEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"product.created.v1", "product.updated.v1", "product.deleted.v1", "product.viewed.v1"},
        groupId = "commerce-streamer-cache-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaProductCacheEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductCacheEventHandler productCacheEventHandler;

    @KafkaHandler
    public void handleCreated(ConsumerRecord<String, ProductEvents.Created> record, Acknowledgment ack) {
        log.info("KafkaProductCacheEventConsumer: ProductEvents.Created 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", productCacheEventHandler::handleCreated);
    }

    @KafkaHandler
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("KafkaProductCacheEventConsumer: ProductEvents.Updated 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", productCacheEventHandler::handleUpdated);
    }

    @KafkaHandler
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("KafkaProductCacheEventConsumer: ProductEvents.Deleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", productCacheEventHandler::handleDeleted);
    }

    @KafkaHandler
    public void handleViewed(ConsumerRecord<String, ProductEvents.Viewed> record, Acknowledgment ack) {
        log.info("KafkaProductCacheEventConsumer: ProductEvents.Viewed 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productcache", productCacheEventHandler::handleViewed);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        String typeId = extractTypeId(record);
        Object value = record.value();
        
        log.warn("Received unknown event in product topics - topic: {}, partition: {}, offset: {}, __TypeId__: {}, valueType: {}, value: {}", 
                record.topic(), record.partition(), record.offset(), typeId,
                value != null ? value.getClass().getName() : "null", value);
        
        ack.acknowledge();
    }
    
    private String extractTypeId(ConsumerRecord<Object, Object> record) {
        if (record.headers() == null) {
            return null;
        }
        var typeIdHeader = record.headers().lastHeader("__TypeId__");
        return typeIdHeader != null ? new String(typeIdHeader.value(), java.nio.charset.StandardCharsets.UTF_8) : null;
    }
}

