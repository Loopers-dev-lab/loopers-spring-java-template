package com.loopers.domain.metrics.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface ProductMetricsRepository {
    Optional<ProductMetrics> findByProductId(Long productId);

    Collection<ProductMetrics> findByProductIds(Collection<Long> productIds);

    Page<ProductMetrics> findAll(Pageable pageable);
}
