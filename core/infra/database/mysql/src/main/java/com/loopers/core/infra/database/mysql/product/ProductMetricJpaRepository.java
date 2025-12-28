package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.DailyProductMetricEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductMetricJpaRepository extends JpaRepository<DailyProductMetricEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pme from DailyProductMetricEntity pme where pme.productId = :productId and CAST(pme.createdAt AS date) = CAST(:createdAt AS date)")
    Optional<DailyProductMetricEntity> findByProductIdWithLock(Long productId, LocalDateTime createdAt);
}
