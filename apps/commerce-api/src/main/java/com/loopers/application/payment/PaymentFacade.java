package com.loopers.application.payment;

import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.order.event.PaymentCompletedEvent;
import com.loopers.domain.order.event.PaymentFailedEvent;
import com.loopers.infrastructure.pg.PgStatus;
import com.loopers.infrastructure.pg.PgTransactionService;
import com.loopers.infrastructure.pg.dto.PgTransactionDetail;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private final PaymentDomainService paymentDomainService;
    private final PgTransactionService pgTransactionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void completePaymentByCallback(String pgTransactionId) {
        log.info("Completing payment by callback. pgTransactionId: {}", pgTransactionId);

        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsSuccess(payment.getId(), pgTransactionId);

        eventPublisher.publishEvent(PaymentCompletedEvent.from(payment));

        log.info("Payment completed. orderId: {}", payment.getOrderId());
    }

    @Transactional
    public void failPaymentByCallback(String pgTransactionId, String reason) {
        log.info("Failing payment by callback. pgTransactionId: {}, reason: {}",
                pgTransactionId, reason);

        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsFailed(payment.getId(), reason);

        eventPublisher.publishEvent(PaymentFailedEvent.from(payment));

        log.info("Payment failed. orderId: {}", payment.getOrderId());
    }

    @Transactional(readOnly = true)
    public Payment getPaymentStatus(String userId, Long orderId) {
        Payment payment = paymentDomainService.getPaymentByOrderId(orderId);
        validatePaymentOwner(payment, userId);
        return payment;
    }

    @Transactional
    public Payment syncPaymentStatusWithPG(String userId, Long orderId) {
        Payment payment = paymentDomainService.getPaymentByOrderId(orderId);
        validatePaymentOwner(payment, userId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }

        try {
            log.info("Syncing payment status with PG. pgTransactionId: {}", payment.getPgTransactionId());
            PgTransactionDetail detail = pgTransactionService.getTransaction(userId, payment.getPgTransactionId());

            PgStatus pgStatus = PgStatus.from(detail.getStatus());

            if (pgStatus.isSuccess()) {
                paymentDomainService.markAsSuccess(payment.getId(), payment.getPgTransactionId());
                eventPublisher.publishEvent(PaymentCompletedEvent.from(payment));
                log.info("Payment synced to SUCCESS. orderId: {}", orderId);
            } else if (pgStatus.isFailed()) {
                paymentDomainService.markAsFailed(payment.getId(), detail.getReason());
                eventPublisher.publishEvent(PaymentFailedEvent.from(payment));
                log.info("Payment synced to FAILED. orderId: {}", orderId);
            }

            return paymentDomainService.getPaymentByOrderId(orderId);
        } catch (Exception e) {
            log.warn("Failed to sync payment status with PG. pgTransactionId: {}, error: {}",
                    payment.getPgTransactionId(), e.getMessage());
            return payment;
        }
    }

    private void validatePaymentOwner(Payment payment, String userId) {
        if (!payment.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PAYMENT_ACCESS_DENIED);
        }
    }
}
