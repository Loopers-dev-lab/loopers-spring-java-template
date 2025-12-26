package com.loopers.domain.metrics.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsRepository {
    Optional<ProductMetrics> findByProductId(Long productId);

    Optional<ProductMetrics> findByProductIdForUpdate(Long productId);

    Collection<ProductMetrics> findByProductIds(Collection<Long> productIds);

    Page<ProductMetrics> findAll(List<Long> brandIds, Pageable pageable);

    ProductMetrics save(ProductMetrics productMetrics);

    List<ProductMetrics> saveAll(Collection<ProductMetrics> list);
}
