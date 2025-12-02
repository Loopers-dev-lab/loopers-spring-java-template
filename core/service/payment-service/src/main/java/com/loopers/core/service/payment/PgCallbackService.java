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

/**
 * 결제 게이트웨이 콜백 처리 서비스
 * <p>
 * 결제 게이트웨이에서 받은 콜백 정보를 처리하며,
 * 결제 상황에 따라 적절한 전략을 선택하여 처리합니다.
 * <p>
 * 책임:
 * - 결제 콜백 정보의 기본 값 변환 (TransactionKey, OrderKey 등)
 * - 비관적 락을 통한 Payment 조회
 * - 결제 처리 전략 선택
 * - 트랜잭션 관리
 */
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
