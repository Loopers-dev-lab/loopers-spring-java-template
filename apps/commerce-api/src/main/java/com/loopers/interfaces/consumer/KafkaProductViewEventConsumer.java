package com.loopers.interfaces.consumer;

import com.loopers.domain.product.event.ProductViewEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 ProductView 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 ProductViewEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"product.v1", "like.v1"},
        groupId = "commerce-api-productview-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaProductViewEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductViewEventHandler productViewEventHandler;

    @KafkaHandler
    public void handleCreated(ConsumerRecord<String, ProductEvents.Created> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: ProductEvents.Created 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleCreated);
    }

    @KafkaHandler
    public void handleUpdated(ConsumerRecord<String, ProductEvents.Updated> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: ProductEvents.Updated 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleUpdated);
    }

    @KafkaHandler
    public void handleDeleted(ConsumerRecord<String, ProductEvents.Deleted> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: ProductEvents.Deleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleDeleted);
    }

    @KafkaHandler
    public void handleLikeCountChanged(ConsumerRecord<String, LikeEvents.LikeCountChanged> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: LikeCountChanged 수신 - productId: {}, delta: {}", 
                record.value().productId(), record.value().delta());
        
        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleLikeCountChanged);
    }

    @KafkaHandler
    public void handleProductLikeSaved(ConsumerRecord<String, LikeEvents.ProductLikeSaved> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: ProductLikeSaved 수신 - productId: {}", 
                record.value().productId());
        
        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleProductLikeSaved);
    }

    @KafkaHandler
    public void handleProductLikeDeleted(ConsumerRecord<String, LikeEvents.ProductLikeDeleted> record, Acknowledgment ack) {
        log.info("KafkaProductViewEventConsumer: ProductLikeDeleted 수신 - productId: {}", 
                record.value().productId());
        
        messageProcessor.execute(record, ack, "productview", productViewEventHandler::handleProductLikeDeleted);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        // TypeMapper가 자동으로 역직렬화를 처리하므로, 여기 도달한 것은 알 수 없는 이벤트 타입
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
        return typeIdHeader != null ? new String(typeIdHeader.value(), StandardCharsets.UTF_8) : null;
    }
}

