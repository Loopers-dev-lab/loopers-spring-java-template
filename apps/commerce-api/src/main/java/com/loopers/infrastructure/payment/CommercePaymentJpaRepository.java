package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.CommercePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommercePaymentJpaRepository extends JpaRepository<CommercePayment, Long> {
    Optional<CommercePayment> findByTransactionKey(String transactionKey);
}

