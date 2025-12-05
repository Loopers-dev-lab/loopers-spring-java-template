package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(final CardType cardType, final String cardNo, final Long totalPrice, final Long orderId) {
        return paymentRepository.save(Payment.create(cardType, cardNo, totalPrice, orderId));
    }

    public Payment getPendingPayment(final Long paymentId) {
        return paymentRepository.findByIdAndPaymentStatus(paymentId, PaymentStatus.PENDING)
                .orElseThrow(
                        () -> new CoreException(ErrorType.NOT_FOUND, "[paymentId = " + paymentId + "] 결제 대기중인 결제를 찾을 수 없습니다."));
    }
}
