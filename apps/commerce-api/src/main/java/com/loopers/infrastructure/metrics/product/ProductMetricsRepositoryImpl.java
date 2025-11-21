package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {
    private final ProductMetricsJpaRepository jpaRepository;

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId);
    }

    @Override
    public Collection<ProductMetrics> findByProductIds(Collection<Long> productIds) {
        return jpaRepository.findAllById(productIds);
    }

    @Override
    public Page<ProductMetrics> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }
}
