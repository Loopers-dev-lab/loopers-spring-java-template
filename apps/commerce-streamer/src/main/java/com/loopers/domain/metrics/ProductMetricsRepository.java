package com.loopers.domain.metrics; // Package changed

import com.loopers.domain.metrics.ProductMetrics; // Import changed
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {
    Optional<ProductMetrics> findByProductId(Long productId);
}
