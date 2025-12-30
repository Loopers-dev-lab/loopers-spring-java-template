package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderReference(String orderReference) {
        return paymentRepository.findByOrderReference(orderReference);
    }

    @Transactional(readOnly = true)
    public List<Payment> findPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }
}
