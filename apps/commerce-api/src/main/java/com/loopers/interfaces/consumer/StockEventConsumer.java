package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.stock.event.StockEventPublisher;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"order.created.v1"},
        groupId = "commerce-api-stock-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class StockEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;

    @KafkaHandler
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, OrderEvents.Created> record, Acknowledgment ack) {
        log.info("StockEventConsumer: OrderCreatedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.order", event -> {
            List<StockEvents.OrderItemInfo> orderItems = event.items().stream()
                    .map(item -> new StockEvents.OrderItemInfo(item.productId(), item.quantity()))
                    .collect(Collectors.toList());

            // 데드락 방지를 위해 productId로 정렬
            event.items().stream()
                    .sorted(Comparator.comparing(OrderEvents.OrderItemInfo::productId))
                    .forEach(item -> stockService.decreaseQuantity(item.productId(), (long) item.quantity()));

            log.info("재고 차감 성공 - orderId: {}", event.orderId());
            stockEventPublisher.publishStockProcessed(new StockEvents.Processed(
                    event.orderId(),
                    orderItems,
                    event
            ));
        });
    }

    @KafkaHandler
    @Transactional
    public void handleCouponProcessingFailed(ConsumerRecord<String, com.loopers.domain.coupon.event.CouponEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("StockEventConsumer: CouponProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.coupon", event -> {
            if (event.originalEvent() != null && event.originalEvent().orderItems() != null) {
                compensateStock(event.orderId(), event.originalEvent().orderItems());
            } else {
                log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
            }
        });
    }

    @KafkaHandler
    @Transactional
    public void handlePaymentProcessingFailed(ConsumerRecord<String, com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("StockEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock.payment", event -> {
            if (event.originalEvent() != null 
                    && event.originalEvent().originalEvent() != null 
                    && event.originalEvent().originalEvent().orderItems() != null) {
                compensateStock(event.orderId(), event.originalEvent().originalEvent().orderItems());
            } else {
                log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
            }
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in order.created.v1: {}", record.value());
        ack.acknowledge();
    }

    private void compensateStock(Long orderId, List<StockEvents.OrderItemInfo> orderItems) {
        try {
            orderItems.forEach(item ->
                    stockService.increaseQuantity(item.productId(), (long) item.quantity())
            );
            log.info("재고 원복 성공 - orderId: {}", orderId);
            stockEventPublisher.publishStockCompensated(new StockEvents.Compensated(orderId));
        } catch (Exception e) {
            log.error("재고 원복 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 보상 트랜잭션 실패에 대한 처리 전략 필요 (e.g., 재시도, 로깅, 알림)
        }
    }
}

