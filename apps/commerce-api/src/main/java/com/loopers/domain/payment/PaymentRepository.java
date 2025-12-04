package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByTransactionKey(String transactionKey);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(PaymentStatus status);

    void deleteAll();
}
