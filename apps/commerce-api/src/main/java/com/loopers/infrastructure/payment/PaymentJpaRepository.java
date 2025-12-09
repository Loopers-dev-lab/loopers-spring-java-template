package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
  List<Payment> findByOrderId(Long orderId);
  
  List<Payment> findByStatus(PaymentStatus status);
  
  @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.updatedAt >= :fromDate")
  List<Payment> findByStatusAndUpdatedAtAfter(@Param("status") PaymentStatus status, @Param("fromDate") java.time.ZonedDateTime fromDate);
}