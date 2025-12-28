package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.vo.ProductId;

import java.util.Optional;

public interface ProductMetricRepository {

    Optional<DailyProductMetric> findByWithLock(ProductId productId, CreatedAt createdAt);

    DailyProductMetric save(DailyProductMetric dailyProductMetric);
}
