package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
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
    public Optional<ProductMetrics> findByProductIdForUpdate(Long productId) {
        return jpaRepository.findByProductIdForUpdate(productId);
    }

    @Override
    public Collection<ProductMetrics> findByProductIds(Collection<Long> productIds) {
        return jpaRepository.findAllByProductIdIn(productIds);
    }

    @Override
    public Page<ProductMetrics> findAll(List<Long> brandIds, Pageable pageable) {
        if (brandIds == null || brandIds.isEmpty()) {
            return jpaRepository.findAll(pageable);
        }
        return jpaRepository.findAllByBrandIdIn(brandIds, pageable);
    }

    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return jpaRepository.save(productMetrics);
    }

    @Override
    public List<ProductMetrics> saveAll(Collection<ProductMetrics> list) {
        return jpaRepository.saveAll(list);
    }
}
