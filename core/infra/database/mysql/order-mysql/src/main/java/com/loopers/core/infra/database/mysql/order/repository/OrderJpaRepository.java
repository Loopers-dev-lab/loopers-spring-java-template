package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.infra.database.mysql.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
