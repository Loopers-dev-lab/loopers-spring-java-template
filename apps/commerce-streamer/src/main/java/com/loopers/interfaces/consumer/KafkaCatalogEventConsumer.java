package com.loopers.interfaces.consumer;

import com.loopers.domain.product.event.CatalogEventHandler;
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
 * Kafka 기반 카탈로그 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 CatalogEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"product.updated.v1", "product.deleted.v1", "product.like-count-changed.v1"},
        groupId = "commerce-streamer-catalog-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaCatalogEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final CatalogEventHandler catalogEventHandler;

    @KafkaHandler
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("KafkaCatalogEventConsumer: ProductEvents.Updated 수신 - productId: {}", record.value().productId());

        messageProcessor.execute(record, ack, "catalog", catalogEventHandler::handleUpdated);
    }

    @KafkaHandler
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("KafkaCatalogEventConsumer: ProductEvents.Deleted 수신 - productId: {}", record.value().productId());

        messageProcessor.execute(record, ack, "catalog", catalogEventHandler::handleDeleted);
    }

    @KafkaHandler
    public void handleLikeCount(ConsumerRecord<String, ProductEvents.LikeCount> record, Acknowledgment ack) {
        log.info("KafkaCatalogEventConsumer: ProductEvents.LikeCount 수신 - productId: {} (delta: {})", 
                record.value().productId(), record.value().delta());

        messageProcessor.execute(record, ack, "catalog", catalogEventHandler::handleLikeCount);
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

