package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.service.order.component.PayedOrderFailHandler;
import com.loopers.core.service.payment.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayedOrderHandler {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PayedOrderFailHandler payedOrderFailHandler;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PaymentCompletedEvent event) {
        try {
            Payment payment = paymentRepository.getById(event.paymentId());
            Order order = orderRepository.getBy(payment.getOrderKey());
            orderRepository.save(order.payed());
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            payedOrderFailHandler.handle(event, exception);
        }
    }
}
