package com.loopers.domain.stock.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.order.OrderDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final StockService stockService;
    private final StockEventPublisher stockEventPublisher;
    private final OrderService orderService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderEvents.Created event) {
        log.info("StockEventListener: OrderCreatedEvent 수신 - orderId: {}", event.orderId());
        try {
            event.request().items().stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId)) // 데드락 방지
                .forEach(item -> stockService.decreaseQuantity(item.productId(), (long) item.quantity()));

            log.info("재고 차감 성공 - orderId: {}", event.orderId());
            stockEventPublisher.publishStockProcessed(new StockEvents.Processed(event.orderId(), event));

        } catch (Exception e) {
            log.error("재고 차감 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
            stockEventPublisher.publishStockProcessingFailed(new StockEvents.ProcessingFailed(event.orderId(), e.getMessage()));
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("StockEventListener: CouponProcessingFailedEvent 수신 - orderId: {}", event.orderId());
        compensateStock(event.orderId());
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("StockEventListener: PaymentProcessingFailedEvent 수신 - orderId: {}", event.orderId());
        compensateStock(event.orderId());
    }

    private void compensateStock(Long orderId) {
        try {
            Order order = orderService.findOrderById(orderId);
            order.getOrderItems().forEach(item ->
                    stockService.increaseQuantity(item.getProductId(), (long) item.getQuantity())
            );
            log.info("재고 원복 성공 - orderId: {}", orderId);
            stockEventPublisher.publishStockCompensated(new StockEvents.Compensated(orderId));
        } catch (Exception e) {
            log.error("재고 원복 실패 - orderId: {}, error: {}", orderId, e.getMessage());
            // 보상 트랜잭션 실패에 대한 처리 전략 필요 (e.g., 재시도, 로깅, 알림)
        }
    }
}
