package com.loopers.core.service.payment;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.service.payment.command.PgCallbackCommand;
import com.loopers.core.service.payment.component.PaymentCallbackStrategy;
import com.loopers.core.service.payment.component.PaymentCallbackStrategySelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PgCallbackService {

    private final PaymentRepository paymentRepository;
    private final PaymentCallbackStrategySelector strategySelector;

    @Transactional
    public void callback(PgCallbackCommand command) {
        TransactionKey transactionKey = new TransactionKey(command.transactionKey());
        OrderKey orderKey = new OrderKey(command.orderId());
        PaymentStatus status = PaymentStatus.valueOf(command.status());
        FailedReason failedReason = new FailedReason(command.reason());

        Payment payment = paymentRepository.getByWithLock(transactionKey);
        PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);
        Payment processedPayment = strategy.pay(payment, failedReason);
        paymentRepository.save(processedPayment);
    }
}
