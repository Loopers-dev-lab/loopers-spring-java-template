package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.infra.database.mysql.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {
}
