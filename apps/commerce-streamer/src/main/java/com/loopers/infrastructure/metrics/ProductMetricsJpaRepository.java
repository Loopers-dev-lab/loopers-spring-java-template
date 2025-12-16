package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductMetricsEntity;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsEntity, Long> {
}
