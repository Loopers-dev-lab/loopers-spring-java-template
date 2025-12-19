package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEventPublisher;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
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
        topics = {"payment.completed.v1", "stock.deduction-failed.v1", "coupon.apply-failed.v1", "payment.failed.v1"},
        groupId = "commerce-api-order-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class OrderEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

    @KafkaHandler
    @Transactional
    public void handlePaymentProcessed(ConsumerRecord<String, PaymentEvents.Processed> record, Acknowledgment ack) {
        log.info("OrderEventConsumer: PaymentProcessedEvent 수신 - orderId: {}",
                record.value().orderId());

        messageProcessor.execute(record, ack, "order.payment", event -> {
            log.info("Saga 최종 성공 처리 - orderId: {}", event.orderId());
            var order = orderService.saveSuccessOrder(event.orderId());

            // 주문 완료 이벤트 발행 (데이터 플랫폼 전송용)
            orderEventPublisher.publishOrderConfirmed(
                    new OrderEvents.Confirmed(
                            order.getId(),
                            order.getUserId(),
                            order.getOrderStatus().name()
                    )
            );
        });
    }

    @KafkaHandler
    @Transactional
    public void handleStockProcessingFailed(ConsumerRecord<String, StockEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("OrderEventConsumer: StockProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.stock", event -> {
            log.info("Saga 최종 실패 처리 (Stock) - orderId: {}, reason: {}", event.orderId(), event.reason());
            orderService.saveFailedOrder(event.orderId(), event.reason());
        });
    }

    @KafkaHandler
    @Transactional
    public void handleCouponProcessingFailed(ConsumerRecord<String, CouponEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("OrderEventConsumer: CouponProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.coupon", event -> {
            log.info("Saga 최종 실패 처리 (Coupon) - orderId: {}, reason: {}", event.orderId(), event.reason());
            orderService.saveFailedOrder(event.orderId(), event.reason());
        });
    }

    @KafkaHandler
    @Transactional
    public void handlePaymentProcessingFailed(ConsumerRecord<String, PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("OrderEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}, reason: {}",
                record.value().orderId(), record.value().reason());

        messageProcessor.execute(record, ack, "order.payment", event -> {
            log.info("Saga 최종 실패 처리 (Payment) - orderId: {}, reason: {}", event.orderId(), event.reason());
            orderService.saveFailedOrder(event.orderId(), event.reason());
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in order topics: {}", record.value());
        ack.acknowledge();
    }
}

