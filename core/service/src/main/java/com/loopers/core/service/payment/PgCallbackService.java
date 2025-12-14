package com.loopers.core.service.payment;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PgPaymentRepository;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PaymentCompletedEvent;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.service.payment.command.PgCallbackCommand;
import com.loopers.core.service.payment.component.PaymentCallbackStrategy;
import com.loopers.core.service.payment.component.PaymentCallbackStrategySelector;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PgCallbackService {

    private final PgPaymentRepository pgPaymentRepository;
    private final PaymentCallbackStrategySelector strategySelector;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void callback(PgCallbackCommand command) {
        TransactionKey transactionKey = new TransactionKey(command.transactionKey());
        OrderKey orderKey = new OrderKey(command.orderId());
        PgPaymentStatus status = PgPaymentStatus.valueOf(command.status());
        FailedReason failedReason = new FailedReason(command.reason());

        PgPayment pgPayment = pgPaymentRepository.getByWithLock(transactionKey);
        pgPaymentRepository.save(pgPayment.with(status, failedReason));
        PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);
        Payment payment = strategy.pay(pgPayment, failedReason);

        eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId()));
    }
}
