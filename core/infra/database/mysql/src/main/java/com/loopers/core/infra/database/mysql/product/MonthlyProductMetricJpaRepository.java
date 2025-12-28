package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.MonthlyProductMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyProductMetricJpaRepository extends JpaRepository<MonthlyProductMetricEntity, Long> {
}
