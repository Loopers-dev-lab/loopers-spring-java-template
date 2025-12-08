package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByTransactionKey(String transactionKey);

  Optional<Payment> findByOrderId(Long orderId);

  List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime before);
}
