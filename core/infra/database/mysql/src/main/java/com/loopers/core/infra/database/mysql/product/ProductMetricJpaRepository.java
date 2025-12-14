package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.ProductMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMetricJpaRepository extends JpaRepository<ProductMetricEntity, Long> {
}
