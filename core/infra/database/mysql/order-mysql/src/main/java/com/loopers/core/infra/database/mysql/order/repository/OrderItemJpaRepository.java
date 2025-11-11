package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.infra.database.mysql.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findAllByOrderId(Long orderId);
}
