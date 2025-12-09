package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
  Payment save(Payment payment);
  
  Optional<Payment> findById(Long id);
  
  List<Payment> findAllByOrderId(Long orderId);
  
  Optional<Payment> findByOrderId(Long orderId);
  
  List<Payment> findByStatus(PaymentStatus status);
  
  List<Payment> findRecentFailedPayments(int hoursAgo);
  
  void deleteById(Long id);
}