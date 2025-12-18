package com.loopers.domain.metrics;

import java.util.Optional;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface ProductMetricsRepository {
    ProductMetricsEntity save(ProductMetricsEntity metrics);

    Optional<ProductMetricsEntity> findById(Long productId);
    
    void deleteAll();
}
