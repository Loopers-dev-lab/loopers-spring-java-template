package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long>, PaymentRepository {

    @Override
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.deletedAt IS NULL")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);

    @Override
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.deletedAt IS NULL")
    List<Payment> findByOrderId(@Param("orderId") String orderId);
}
