package com.loopers.interfaces.listener;

import com.loopers.application.order.OrderEvent;
import com.loopers.application.payment.PaymentEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentSuccessListener {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentEvent.PaymentCompleted event) {
        log.info("PaymentCompleted 이벤트 수신 - 주문 결제 완료 처리: orderKey={}, transactionKey={}, amount={}",
                event.orderKey(), event.transactionKey(), event.amount());

        Order order = orderService.getOrderByOrderKey(event.orderKey());
        orderService.payOrder(order);

        Payment payment = paymentService.getPaymentByOrderKey(event.orderKey());
        payment.updateStatus(PaymentStatus.COMPLETED);
        payment.updateTransactionKey(event.transactionKey());

        eventPublisher.publishEvent(OrderEvent.OrderPaid.from(order));
    }
}

