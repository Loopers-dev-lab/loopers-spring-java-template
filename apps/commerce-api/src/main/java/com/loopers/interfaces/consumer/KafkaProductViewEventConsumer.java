package com.loopers.interfaces.consumer;

import com.loopers.domain.product.event.ProductViewEventHandler;
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
 * Kafka 기반 ProductView 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 ProductViewEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"product.created.v1", "product.updated.v1", "product.deleted.v1"},
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

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in product topics: {}", record.value());
        ack.acknowledge();
    }
}

