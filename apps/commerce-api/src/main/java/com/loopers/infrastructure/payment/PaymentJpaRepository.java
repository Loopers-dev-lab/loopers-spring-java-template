package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    @Modifying
    @Query("""
        UPDATE Payment p
           SET p.idempotencyKey = :idempotencyKey
         WHERE p.orderId = :orderId
    """)
    void saveIdempotencyKeyByOrderId(Long orderId, String idempotencyKey);
}
