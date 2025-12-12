package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderKey(String orderKey);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :before AND p.deletedAt IS NULL")
    List<Payment> findPendingPaymentsCreatedBefore(
            @Param("status") PaymentStatus status,
            @Param("before") ZonedDateTime before
    );
}

