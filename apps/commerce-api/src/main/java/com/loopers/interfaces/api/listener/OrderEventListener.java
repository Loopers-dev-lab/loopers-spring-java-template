package com.loopers.interfaces.api.listener;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.infrastructure.kafka.dto.OrderEventDto;
import com.loopers.infrastructure.kafka.dto.StockChangedDto;
import com.loopers.infrastructure.kafka.producer.OrderEventProducer;
import com.loopers.infrastructure.kafka.producer.StockChangedEventProducer;
import com.loopers.infrastructure.platform.DataPlatformSender;
import com.loopers.infrastructure.platform.OrderResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderFacade orderFacade;
    private final OutboxService outboxService;
    private final DataPlatformSender dataPlatformSender;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderEventProducer orderEventProducer;
    private final StockChangedEventProducer stockChangedEventProducer;

    @Value("${kafka.topic.order-events-name}")
    private String orderEventsTopic;

    @Value("${kafka.topic.product-stock-name}")
    private String productStockTopic;

    /**
     * 주문 생성 후 쿠폰 사용 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponUsage(OrderCreatedEvent event) {
        if (event.couponId() == null) {
            return;
        }

        log.info("쿠폰 사용 처리 시작: orderId={}, couponId={}",
                event.orderId(), event.couponId());

        try {
            orderFacade.useCoupon(event.couponId());
            log.info("쿠폰 사용 처리 완료: couponId={}", event.couponId());
        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패: orderId={}, couponId={}, reason={}",
                    event.orderId(), event.couponId(), e.getMessage());
        }
    }

    /**
     * 주문 생성 후 데이터 플랫폼 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDataPlatformSend(OrderCreatedEvent event) {
        log.info("주문 생성 데이터 플랫폼 전송: orderId={}, loginId={}", event.orderId(), event.userId());

        try {
            List<OrderResultMessage.OrderItemInfo> items = event.items().stream()
                    .map(item -> new OrderResultMessage.OrderItemInfo(
                            item.productId(), item.quantity(), item.unitPrice()))
                    .toList();

            OrderResultMessage message = OrderResultMessage.created(
                    event.orderId(),
                    event.userId(),
                    event.totalAmount(),
                    event.discountAmount(),
                    items
            );

            dataPlatformSender.sendOrderResult(message);
        } catch (Exception e) {
            log.error("주문 데이터 플랫폼 전송 실패: orderId={}, reason={}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * 주문 생성 후 Kafka 이벤트 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleKafkaEventPublish(OrderCreatedEvent event) {
        log.info("주문 생성 Outbox 이벤트 저장: orderId={}", event.orderId());

        List<OrderEventDto.OrderItemDto> items = event.items().stream()
                .map(item -> new OrderEventDto.OrderItemDto(
                        item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        OrderEventDto dto = OrderEventDto.created(
                event.orderId(),
                event.userId(),
                event.totalAmount(),
                event.discountAmount(),
                items
        );

        outboxService.saveEvent(
                "ORDER",
                event.orderId().toString(),
                "ORDER_CREATED",
                orderEventsTopic,
                event.orderId().toString(),
                dto
        );
    }

    /**
     * 주문 생성 후 유저 행동 로깅 이벤트 발행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActionLogging(OrderCreatedEvent event) {
        log.debug("주문 생성 유저 행동 로깅: orderId={}, loginId={}", event.orderId(), event.userId());

        eventPublisher.publishEvent(
                UserActionEvent.orderCreate(event.userId(), event.orderId(), event.totalAmount())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleStockChangedOutboxEvent(OrderCreatedEvent event) {
        log.info("재고 변경 Outbox 이벤트 저장: orderId={}", event.orderId());

        for (OrderCreatedEvent.OrderItemInfo item : event.items()) {
            StockChangedDto dto = StockChangedDto.of(
                    item.productId(),
                    item.quantity(),
                    "DECREASED"
            );

            outboxService.saveEvent(
                    "PRODUCT",
                    item.productId().toString(),
                    "STOCK_DECREASED",
                    productStockTopic,
                    item.productId().toString(),
                    dto
            );
        }
    }
}
