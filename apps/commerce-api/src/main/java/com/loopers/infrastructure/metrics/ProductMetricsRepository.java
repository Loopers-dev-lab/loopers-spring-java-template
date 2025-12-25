package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);
}
