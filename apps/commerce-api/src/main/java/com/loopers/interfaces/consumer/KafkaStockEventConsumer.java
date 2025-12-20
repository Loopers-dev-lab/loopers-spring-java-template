package com.loopers.interfaces.consumer;

import com.loopers.domain.stock.event.StockEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
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
 * Kafka 기반 재고 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 StockEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"order.v1", "coupon.v1", "payment.v1"},
        groupId = "commerce-api-stock-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaStockEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final StockEventHandler stockEventHandler;

    @KafkaHandler
    public void handleOrderCreated(ConsumerRecord<String, OrderEvents.Created> record, Acknowledgment ack) {
        log.info("KafkaStockEventConsumer: OrderCreatedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.order", stockEventHandler::handleOrderCreated);
    }

    @KafkaHandler
    public void handleCouponProcessingFailed(ConsumerRecord<String, CouponEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaStockEventConsumer: CouponProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.coupon", stockEventHandler::handleCouponProcessingFailed);
    }

    @KafkaHandler
    public void handlePaymentProcessingFailed(ConsumerRecord<String, PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("KafkaStockEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.payment", stockEventHandler::handlePaymentProcessingFailed);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        String typeId = extractTypeId(record);
        Object value = record.value();
        
        log.warn("Received unknown event in stock topics - topic: {}, partition: {}, offset: {}, __TypeId__: {}, valueType: {}, value: {}", 
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

