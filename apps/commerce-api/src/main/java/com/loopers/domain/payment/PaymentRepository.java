package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    void savePayment(Payment payment);

    Optional<Payment> findByOrderKey(String orderKey);

    List<Payment> getPendingPaymentsCreatedBefore(ZonedDateTime before);
}

