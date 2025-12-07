package com.loopers.core.infra.database.mysql.payment.repository;

import com.loopers.core.infra.database.mysql.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByOrderKeyAndStatus(String orderKey, String status);

}
