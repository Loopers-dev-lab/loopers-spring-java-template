package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {
    Optional<ProductMetrics> findByProductId(Long productId);
}
