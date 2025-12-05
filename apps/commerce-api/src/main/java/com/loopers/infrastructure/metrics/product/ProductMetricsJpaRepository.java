package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetrics;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {
    Optional<ProductMetrics> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.productId = :productId")
    Optional<ProductMetrics> findByProductIdForUpdate(Long productId);

    Page<ProductMetrics> findAllByBrandIdIn(List<Long> brandIds, Pageable pageable);

    List<ProductMetrics> findAllByProductIdIn(Collection<Long> productIds);
}
