package com.loopers.application.order;

import com.loopers.domain.coupon.event.CouponProcessEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.SagaStatus;
import com.loopers.domain.order.event.OrderCompensationEvent;
import com.loopers.domain.order.event.OrderEventPublisher;
import com.loopers.domain.payment.event.PaymentProcessEvent;
import com.loopers.domain.stock.event.StockProcessEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderSagaManager {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaManager.class);

    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcess(StockProcessEvent event) {
        log.info("SagaManager: StockProcessEvent 수신 - orderId: {}, success: {}", event.orderId(), event.isSuccess());
        Order order = orderService.findOrderById(event.orderId());

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("이미 처리되었거나 실패한 주문입니다. orderId: {}", event.orderId());
            return;
        }

        if (event.isSuccess()) {
            order.updateStockSagaStatus(SagaStatus.SUCCESS);
        } else {
            order.updateStockSagaStatus(SagaStatus.FAILED);
            order.fail(event.reason());
            orderEventPublisher.publishOrderCompensation(new OrderCompensationEvent(event.orderId()));
        }
        orderService.saveOrder(order);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcess(CouponProcessEvent event) {
        log.info("SagaManager: CouponProcessEvent 수신 - orderId: {}, success: {}", event.orderId(), event.isSuccess());
        Order order = orderService.findOrderById(event.orderId());

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("이미 처리되었거나 실패한 주문입니다. orderId: {}", event.orderId());
            return;
        }

        if (event.isSuccess()) {
            order.updateCouponSagaStatus(SagaStatus.SUCCESS);
        } else {
            order.updateCouponSagaStatus(SagaStatus.FAILED);
            order.fail(event.reason());
            orderEventPublisher.publishOrderCompensation(new OrderCompensationEvent(event.orderId()));
        }
        orderService.saveOrder(order);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcess(PaymentProcessEvent event) {
        log.info("SagaManager: PaymentProcessEvent 수신 - orderId: {}, success: {}", event.orderId(), event.isSuccess());
        Order order = orderService.findOrderById(event.orderId());

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("이미 처리되었거나 실패한 주문입니다. orderId: {}", event.orderId());
            return;
        }

        if (event.isSuccess()) {
            order.updatePaymentSagaStatus(SagaStatus.SUCCESS);
        } else {
            order.updatePaymentSagaStatus(SagaStatus.FAILED);
            order.fail(event.reason());
            orderEventPublisher.publishOrderCompensation(new OrderCompensationEvent(event.orderId()));
        }
        orderService.saveOrder(order);
    }
}
