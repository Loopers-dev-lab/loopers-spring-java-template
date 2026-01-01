package com.loopers.infrastructure;

import com.loopers.domain.ProductMetric;
import com.loopers.domain.ProductMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {
    private final ProductMetricJpaRepository productMetricJpaRepository;

    @Override
    public ProductMetric findByProductIdAndDate(Long productId, String date) {
        return productMetricJpaRepository.findByProductIdAndMetricDate(productId, date);
    }

    @Override
    public ProductMetric save(ProductMetric productMetric) {
        return productMetricJpaRepository.save(productMetric);
    }
}
