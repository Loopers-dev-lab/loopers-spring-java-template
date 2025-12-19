package com.loopers.interfaces.consumer;

import com.loopers.domain.coupon.event.CouponEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
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
 * Kafka 기반 쿠폰 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 CouponEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"stock.v1", "payment.v1"},
        groupId = "commerce-api-coupon-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaCouponEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final CouponEventHandler couponEventHandler;

    @KafkaHandler
    public void handleStockProcessed(ConsumerRecord<String, StockEvents.Processed> record, Acknowledgment ack) {
        log.info("KafkaCouponEventConsumer: StockProcessedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "coupon.stock", couponEventHandler::handleStockProcessed);
    }

    @KafkaHandler
    public void handlePaymentProcessingFailed(ConsumerRecord<String, PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaCouponEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "coupon.payment", couponEventHandler::handlePaymentProcessingFailed);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in coupon topics: {}", record.value());
        ack.acknowledge();
    }
}

