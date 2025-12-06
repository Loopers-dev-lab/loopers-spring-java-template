package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Long orderId, String userId, Long amount, PaymentType paymentType) {
        Payment payment = Payment.create(orderId, userId, amount, paymentType);
        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePgTransactionId(Long paymentId, String pgTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.PAYMENT_NOT_FOUND + " paymentId: " + paymentId
                ));
        payment.updatePgTransactionId(pgTransactionId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsSuccess(Long paymentId, String pgTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.PAYMENT_NOT_FOUND + " paymentId: " + paymentId
                ));
        payment.markAsSuccess(pgTransactionId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsFailed(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.PAYMENT_NOT_FOUND + " paymentId: " + paymentId
                ));
        payment.markAsFailed(reason);
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.PAYMENT_NOT_FOUND + " orderId: " + orderId
                ));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByPgTransactionId(String pgTransactionId) {
        return paymentRepository.findByPgTransactionId(pgTransactionId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.PAYMENT_NOT_FOUND + " pgTransactionId: " + pgTransactionId
                ));
    }
}
