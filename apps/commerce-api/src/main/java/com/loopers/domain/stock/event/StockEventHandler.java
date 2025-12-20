package com.loopers.domain.stock.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.event.InboxEventService;
import com.loopers.infrastructure.stock.event.StockInboxEventRepository;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.StockService;
import com.loopers.support.error.CoreException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 재고 관련 이벤트 핸들러
 * SAGA 패턴의 재고 차감 및 보상 트랜잭션 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventHandler {

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;
    private final MeterRegistry meterRegistry;
    private final StockInboxEventRepository stockInboxEventRepository;
    private final InboxEventService inboxEventService;

    @Transactional(noRollbackFor = CoreException.class)
    public void handleOrderCreated(OrderEvents.Created event) {
        log.info("StockEventHandler: OrderCreatedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                stockInboxEventRepository,
                event,
                "order.v1",
                (eventId, aggregateId, type, topic) -> StockInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

        List<StockEvents.OrderItemInfo> orderItems = event.items().stream()
                .map(item -> new StockEvents.OrderItemInfo(item.productId(), item.quantity()))
                .collect(Collectors.toList());

        // 데드락 방지를 위해 productId로 정렬
        try {
            event.items().stream()
                    .sorted(Comparator.comparing(OrderEvents.OrderItemInfo::productId))
                    .forEach(item -> stockService.decreaseQuantity(item.productId(), (long) item.quantity()));

            log.info("재고 차감 성공 - orderId: {}", event.orderId());
            stockEventPublisher.publishStockProcessed(new StockEvents.Processed(
                    event.orderId(),
                    orderItems,
                    event
            ));
        } catch (Exception e) {
            // 재고 차감 실패 시 실패 이벤트 발행
            log.error("재고 차감 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            stockEventPublisher.publishStockProcessingFailed(new StockEvents.ProcessingFailed(
                    event.orderId(),
                    orderItems,
                    "재고가 부족합니다: " + e.getMessage()
            ));
            // 예외를 다시 던지지 않고 return하여 트랜잭션이 커밋되어 이벤트가 발행되도록 함
            // 보상 트랜잭션은 StockEvents.ProcessingFailed를 받은 다른 핸들러들이 처리함
            return;
        }
    }

    @Transactional
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("StockEventHandler: CouponProcessingFailedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                stockInboxEventRepository,
                event,
                "coupon.v1",
                (eventId, aggregateId, type, topic) -> StockInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

        if (event.originalEvent() != null && event.originalEvent().orderItems() != null) {
            compensateStock(event.orderId(), event.originalEvent().orderItems());
        } else {
            log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
        }
    }

    @Transactional
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("StockEventHandler: PaymentProcessingFailedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                stockInboxEventRepository,
                event,
                "payment.v1",
                (eventId, aggregateId, type, topic) -> StockInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

        if (event.originalEvent() != null
                && event.originalEvent().originalEvent() != null
                && event.originalEvent().originalEvent().orderItems() != null) {
            compensateStock(event.orderId(), event.originalEvent().originalEvent().orderItems());
        } else {
            log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
        }
    }

    private void compensateStock(Long orderId, List<StockEvents.OrderItemInfo> orderItems) {
        try {
            orderItems.forEach(item ->
                    stockService.increaseQuantity(item.productId(), (long) item.quantity())
            );
            log.info("재고 원복 성공 - orderId: {}", orderId);
            stockEventPublisher.publishStockCompensated(new StockEvents.Compensated(orderId));
            meterRegistry.counter("stock.compensation", "status", "success").increment();
        } catch (Exception e) {
            log.error("재고 원복 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 보상 트랜잭션 실패 메트릭 기록
            meterRegistry.counter("stock.compensation", "status", "failure").increment();
            // 보상 트랜잭션 실패에 대한 처리 전략 필요 (e.g., 재시도, 로깅, 알림)
        }
    }
}

