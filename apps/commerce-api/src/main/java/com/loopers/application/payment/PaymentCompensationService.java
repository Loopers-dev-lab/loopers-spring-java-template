package com.loopers.application.payment;

import com.loopers.application.stock.StockRecoveryService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationService {

    private final PaymentDomainService paymentDomainService;
    private final OrderDomainService orderDomainService;
    private final StockRecoveryService stockRecoveryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateFailedPayment(String userId, Long orderId, Long paymentId, String reason) {
        log.info("Compensating failed payment. orderId: {}, paymentId: {}, reason: {}",
                orderId, paymentId, reason);

        Order order = orderDomainService.getOrder(userId, orderId);

        if (order.getStatus() != OrderStatus.PAYING) {
            log.warn("Order is not in PAYING status. Skipping compensation. orderId: {}, status: {}",
                    orderId, order.getStatus());
            return;
        }

        paymentDomainService.markAsFailed(paymentId, reason);
        stockRecoveryService.recover(order);
        order.fail();

        log.info("Payment compensation completed. orderId: {}", orderId);
    }
}
