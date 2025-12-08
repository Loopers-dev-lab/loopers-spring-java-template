package com.loopers.core.infra.database.mysql.payment.repository;

import com.loopers.core.infra.database.mysql.payment.entity.PgPaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PgPaymentJpaRepository extends JpaRepository<PgPaymentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PgPaymentEntity p where p.transactionKey = :transactionKey")
    Optional<PgPaymentEntity> findByTransactionKey(String transactionKey);

    Optional<PgPaymentEntity> findByPaymentId(Long paymentId);
    
}
