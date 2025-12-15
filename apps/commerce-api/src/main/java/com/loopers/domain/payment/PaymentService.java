package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제정보가 존재하지 않습니다. orderId: " + orderId));
    }

    public void saveTransactionKey(Long orderId, String transactionKey) {
        Payment payment = getPaymentByOrderId(orderId);
        paymentRepository.saveIdempotencyKeyByOrderId(orderId, transactionKey);
    }
    public Payment savePayment(Payment payment) {
        return paymentRepository.savePayment(payment);
    }
}
