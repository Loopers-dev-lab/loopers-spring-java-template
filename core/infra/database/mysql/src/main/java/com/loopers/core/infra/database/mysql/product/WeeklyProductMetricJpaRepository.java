package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.WeeklyProductMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyProductMetricJpaRepository extends JpaRepository<WeeklyProductMetricEntity, Long>, WeeklyProductMetricQuerydslRepository {
}
