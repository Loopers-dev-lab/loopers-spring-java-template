package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ProductMetrics JPA Repository
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);
}
