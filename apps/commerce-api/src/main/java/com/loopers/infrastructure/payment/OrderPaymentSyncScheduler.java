package com.loopers.infrastructure.payment;

import com.loopers.application.order.OrderPaymentProcessor;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentSyncScheduler {

    private final PaymentService paymentService;
    private final OrderPaymentProcessor orderPaymentProcessor;

    @Scheduled(fixedDelayString = "${pg.sync.fixed-delay-ms:6000000}")
    public void syncPendingPayments() {
        List<Payment> payments = paymentService.findPendingPayments();
        for (Payment payment : payments) {
            try {
                orderPaymentProcessor.syncPayment(payment.getOrderId());
            } catch (Exception e) {
                log.warn("결제 동기화 실패 orderId={}, reason={}", payment.getOrderId(), e.getMessage());
            }
        }
    }
}
