package com.loopers.job.ranking;

import com.loopers.batch.domain.metrics.ProductMetrics;
import com.loopers.batch.domain.metrics.ProductMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMetricsTestRepository extends JpaRepository<ProductMetrics, ProductMetricsId> {
}
