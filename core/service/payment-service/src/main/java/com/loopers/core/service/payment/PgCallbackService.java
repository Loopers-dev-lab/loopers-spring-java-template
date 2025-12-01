package com.loopers.core.service.payment;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.service.payment.command.PgCallbackCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PgCallbackService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void callback(PgCallbackCommand command) {
        TransactionKey transactionKey = new TransactionKey(command.transactionKey());
        OrderKey orderKey = new OrderKey(command.orderId());
        PaymentStatus status = PaymentStatus.valueOf(command.status());
        Payment payment = paymentRepository.getByWithLock(transactionKey);

        boolean hasSuccessfulPayment = paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS).isPresent();
        if (hasSuccessfulPayment) {
            paymentRepository.save(payment.fail(new FailedReason("이미 결제에 성공한 이력이 있는 주문입니다.")));
            return;
        }

        if (status != PaymentStatus.SUCCESS) {
            paymentRepository.save(payment.fail(new FailedReason(command.reason())));
            return;
        }

        paymentRepository.save(payment.success());
    }
}
