package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.ProductMetric;
import com.loopers.core.domain.product.vo.ProductId;

import java.util.Optional;

public interface ProductMetricRepository {

    Optional<ProductMetric> findByWithLock(ProductId productId);

    ProductMetric save(ProductMetric productMetric);
}
