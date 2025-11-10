package com.loopers.core.infra.database.mysql.payment.repository;

import com.loopers.core.infra.database.mysql.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
}
