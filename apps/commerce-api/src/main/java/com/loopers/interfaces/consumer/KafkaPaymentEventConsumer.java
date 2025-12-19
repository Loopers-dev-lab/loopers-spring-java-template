package com.loopers.interfaces.consumer;

import com.loopers.domain.payment.event.PaymentEventHandler;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.event.CouponEvents;
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
 * Kafka 기반 결제 이벤트 Consumer
 * 얇은 어댑터 역할만 수행하며, 실제 비즈니스 로직은 PaymentEventHandler에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.consumer.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(
        topics = {"payment.v1", "coupon.v1"},
        groupId = "commerce-api-payment-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class KafkaPaymentEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final PaymentEventHandler paymentEventHandler;

    @KafkaHandler
    public void handlePaymentCallbackReceived(ConsumerRecord<String, PaymentEvents.CallbackReceived> record, Acknowledgment ack) {
        log.info("KafkaPaymentEventConsumer: PaymentCallbackReceivedEvent 수신 - orderId: {}, transactionKey: {}, status: {}",
                record.value().orderId(), record.value().transactionKey(), record.value().status());

        messageProcessor.execute(record, ack, "payment.callback", paymentEventHandler::handlePaymentCallbackReceived);
    }

    @KafkaHandler
    public void handleCouponProcessed(ConsumerRecord<String, CouponEvents.Processed> record, Acknowledgment ack) {
        log.info("KafkaPaymentEventConsumer: CouponProcessedEvent 수신 - orderId: {}",
                record.value().orderId());

        messageProcessor.execute(record, ack, "payment.coupon", paymentEventHandler::handleCouponProcessed);
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in payment topics: {}", record.value());
        ack.acknowledge();
    }
}

