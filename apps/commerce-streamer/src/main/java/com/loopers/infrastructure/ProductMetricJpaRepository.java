package com.loopers.infrastructure;

import com.loopers.domain.ProductMetric;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductMetricJpaRepository extends JpaRepository<ProductMetric, Long> {
    ProductMetric findByProductId(Long productId);

    ProductMetric findByProductIdAndMetricDate(Long productId, String date);
}
