package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

  private final PaymentJpaRepository jpaRepository;

  @Override
  public Payment save(Payment payment) {
    return jpaRepository.save(payment);
  }

  @Override
  public Optional<Payment> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Optional<Payment> findByTransactionKey(String transactionKey) {
    return jpaRepository.findByTransactionKey(transactionKey);
  }

  @Override
  public List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime before) {
    return jpaRepository.findByStatusAndPgRequestedAtBefore(status, before);
  }
}
