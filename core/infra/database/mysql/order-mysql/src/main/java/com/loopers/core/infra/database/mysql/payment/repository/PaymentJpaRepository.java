package com.loopers.core.infra.database.mysql.payment.repository;

import com.loopers.core.infra.database.mysql.payment.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.transactionKey = :transactionKey")
    Optional<PaymentEntity> findByTransactionKeyWithLock(String transactionKey);

    Optional<PaymentEntity> findByOrderKeyAndStatus(String orderKey, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.orderKey = :orderKey and p.status = :status")
    Optional<PaymentEntity> findByOrderKeyAndStatusWithLock(String orderKey, String status);
}
