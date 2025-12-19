package com.loopers.interfaces.consumer;

import com.loopers.domain.order.event.OrderEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.event.CouponEvents;
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
 * Kafka 기반 주문 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 OrderEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"payment.completed.v1", "stock.deduction-failed.v1", "coupon.apply-failed.v1", "payment.failed.v1"},
        groupId = "commerce-api-order-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaOrderEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final OrderEventHandler orderEventHandler;

    @KafkaHandler
    public void handlePaymentProcessed(ConsumerRecord<String, PaymentEvents.Processed> record, Acknowledgment ack) {
        log.info("KafkaOrderEventConsumer: PaymentProcessedEvent 수신 - orderId: {}",
                record.value().orderId());

        messageProcessor.execute(record, ack, "order.payment", orderEventHandler::handlePaymentProcessed);
    }

    @KafkaHandler
    public void handleStockProcessingFailed(ConsumerRecord<String, StockEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaOrderEventConsumer: StockProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.stock", orderEventHandler::handleStockProcessingFailed);
    }

    @KafkaHandler
    public void handleCouponProcessingFailed(ConsumerRecord<String, CouponEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaOrderEventConsumer: CouponProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.coupon", orderEventHandler::handleCouponProcessingFailed);
    }

    @KafkaHandler
    public void handlePaymentProcessingFailed(ConsumerRecord<String, PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaOrderEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.payment", orderEventHandler::handlePaymentProcessingFailed);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in order topics: {}", record.value());
        ack.acknowledge();
    }
}

