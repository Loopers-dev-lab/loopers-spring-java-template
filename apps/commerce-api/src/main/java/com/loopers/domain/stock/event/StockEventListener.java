package com.loopers.domain.stock.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.order.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderEvents.Created event) {
        log.info("StockEventListener: OrderCreatedEvent 수신 - orderId: {}", event.orderId());
        try {
            // OrderItems 정보 추출 (재고 원복을 위해 필요)
            List<StockEvents.OrderItemInfo> orderItems = event.request().items().stream()
                .map(item -> new StockEvents.OrderItemInfo(item.productId(), item.quantity()))
                .toList();
            
            event.request().items().stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId)) // 데드락 방지
                .forEach(item -> stockService.decreaseQuantity(item.productId(), (long) item.quantity()));

            log.info("재고 차감 성공 - orderId: {}", event.orderId());
            stockEventPublisher.publishStockProcessed(new StockEvents.Processed(
                event.orderId(), 
                orderItems,
                event
            ));

        } catch (Exception e) {
            log.error("재고 차감 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
            // 실패 시에도 orderItems 정보를 포함하여 재고 원복 가능하도록 함
            List<StockEvents.OrderItemInfo> orderItems = event.request().items().stream()
                .map(item -> new StockEvents.OrderItemInfo(item.productId(), item.quantity()))
                .toList();
            stockEventPublisher.publishStockProcessingFailed(new StockEvents.ProcessingFailed(
                event.orderId(), 
                orderItems,
                e.getMessage()
            ));
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("StockEventListener: CouponProcessingFailedEvent 수신 - orderId: {}", event.orderId());
        // originalEvent를 통해 orderItems 정보 접근
        if (event.originalEvent() != null && event.originalEvent().orderItems() != null) {
            compensateStock(event.orderId(), event.originalEvent().orderItems());
        } else {
            log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("StockEventListener: PaymentProcessingFailedEvent 수신 - orderId: {}", event.orderId());
        // originalEvent를 통해 orderItems 정보 접근
        if (event.originalEvent() != null 
            && event.originalEvent().originalEvent() != null 
            && event.originalEvent().originalEvent().orderItems() != null) {
            compensateStock(event.orderId(), event.originalEvent().originalEvent().orderItems());
        } else {
            log.warn("재고 원복을 위한 orderItems 정보가 없습니다 - orderId: {}", event.orderId());
        }
    }

    /**
     * 재고 원복 처리
     * @param orderId 주문 ID
     * @param orderItems 원복할 주문 항목 정보
     */
    private void compensateStock(Long orderId, List<StockEvents.OrderItemInfo> orderItems) {
        try {
            orderItems.forEach(item ->
                    stockService.increaseQuantity(item.productId(), (long) item.quantity())
            );
            log.info("재고 원복 성공 - orderId: {}", orderId);
            stockEventPublisher.publishStockCompensated(new StockEvents.Compensated(orderId));
        } catch (Exception e) {
            log.error("재고 원복 실패 - orderId: {}, error: {}", orderId, e.getMessage());
            // 보상 트랜잭션 실패에 대한 처리 전략 필요 (e.g., 재시도, 로깅, 알림)
        }
    }
}
