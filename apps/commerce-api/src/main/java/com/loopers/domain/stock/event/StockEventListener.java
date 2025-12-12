package com.loopers.domain.stock.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.order.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderEvents.Created event) {
        log.info("StockEventListener: OrderCreatedEvent 수신 - orderId: {}", event.orderId());
        
        // OrderItems 정보 추출 (재고 원복을 위해 필요, 트랜잭션 외부에서 미리 추출)
        List<StockEvents.OrderItemInfo> orderItems = event.request().items().stream()
            .map(item -> new StockEvents.OrderItemInfo(item.productId(), item.quantity()))
            .toList();
        
        try {
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
            log.error("재고 차감 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            // 실패 시 이벤트 발행 (트랜잭션 롤백 후에도 이벤트 발행이 가능하도록 별도 메서드로 분리)
            publishStockProcessingFailed(event.orderId(), orderItems, e.getMessage());
        }
    }
    
    /**
     * 재고 처리 실패 이벤트 발행 (트랜잭션 없이 실행)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private void publishStockProcessingFailed(Long orderId, List<StockEvents.OrderItemInfo> orderItems, String reason) {
        try {
            stockEventPublisher.publishStockProcessingFailed(new StockEvents.ProcessingFailed(
                orderId, 
                orderItems,
                reason
            ));
            log.info("StockEvents.ProcessingFailed 발행 성공 - orderId: {}", orderId);
        } catch (Exception publishException) {
            log.error("StockEvents.ProcessingFailed 발행 실패 - orderId: {}, error: {}", 
                    orderId, publishException.getMessage(), publishException);
            // 이벤트 발행 실패는 로깅만 하고 예외를 다시 던지지 않음
        }
    }

    @Async
    @EventListener
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
    @EventListener
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
