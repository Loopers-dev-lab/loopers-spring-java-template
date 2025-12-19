package com.loopers.interfaces.consumer;

import com.loopers.domain.like.event.LikeEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.like.event.LikeEvents;
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
 * Kafka 기반 좋아요 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 LikeEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"like.product-saved.v1", "like.product-deleted.v1", "like.count-changed.v1"},
        groupId = "commerce-streamer-like-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaLikeEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final LikeEventHandler likeEventHandler;

    @KafkaHandler
    public void handleProductLikeSaved(ConsumerRecord<String, LikeEvents.ProductLikeSaved> record, Acknowledgment ack) {
        log.info("KafkaLikeEventConsumer: ProductLikeSaved 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "like", likeEventHandler::handleProductLikeSaved);
    }

    @KafkaHandler
    public void handleProductLikeDeleted(ConsumerRecord<String, LikeEvents.ProductLikeDeleted> record, Acknowledgment ack) {
        log.info("KafkaLikeEventConsumer: ProductLikeDeleted 수신 - productId: {}", 
                record.value().productId());

        messageProcessor.execute(record, ack, "like", likeEventHandler::handleProductLikeDeleted);
    }

    @KafkaHandler
    public void handleLikeCountChanged(ConsumerRecord<String, LikeEvents.LikeCountChanged> record, Acknowledgment ack) {
        log.info("KafkaLikeEventConsumer: LikeCountChanged 수신 - productId: {}, delta: {}", 
                record.value().productId(), record.value().delta());

        messageProcessor.execute(record, ack, "like", likeEventHandler::handleLikeCountChanged);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in like topics: {}", record.value());
        ack.acknowledge();
    }
}

