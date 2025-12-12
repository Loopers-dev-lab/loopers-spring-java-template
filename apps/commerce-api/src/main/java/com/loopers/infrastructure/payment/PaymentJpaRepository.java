package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByTransactionKey(String transactionKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Payment p WHERE p.transactionKey = :transactionKey")
  Optional<Payment> findByTransactionKeyWithLock(@Param("transactionKey") String transactionKey);

  Optional<Payment> findByOrderId(Long orderId);

  List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime before);
}
