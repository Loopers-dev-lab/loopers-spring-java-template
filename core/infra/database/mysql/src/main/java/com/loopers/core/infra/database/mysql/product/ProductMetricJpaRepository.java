package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.ProductMetricEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductMetricJpaRepository extends JpaRepository<ProductMetricEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pme from ProductMetricEntity pme where pme.productId = :productId")
    Optional<ProductMetricEntity> findByProductIdWithLock(Long productId);
}
