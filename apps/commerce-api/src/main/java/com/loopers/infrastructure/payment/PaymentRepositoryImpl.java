package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

  private final PaymentJpaRepository paymentJpaRepository;

  @Override
  public Payment save(Payment payment) {
    return paymentJpaRepository.save(payment);
  }

  @Override
  public Optional<Payment> findById(Long id) {
    return paymentJpaRepository.findById(id);
  }

  @Override
  public List<Payment> findAllByOrderId(Long orderId) {
    return paymentJpaRepository.findByOrderId(orderId);
  }

  @Override
  public Optional<Payment> findByOrderId(Long orderId) {
    return paymentJpaRepository.findByOrderId(orderId).stream().findFirst();
  }

  @Override
  public List<Payment> findByStatus(PaymentStatus status) {
    return paymentJpaRepository.findByStatus(status);
  }

  @Override
  public List<Payment> findRecentFailedPayments(int hoursAgo) {
    ZonedDateTime fromDate = ZonedDateTime.now().minusHours(hoursAgo);
    return paymentJpaRepository.findByStatusAndUpdatedAtAfter(PaymentStatus.FAILED, fromDate);
  }

  @Override
  public void deleteById(Long id) {
    paymentJpaRepository.deleteById(id);
  }
}
