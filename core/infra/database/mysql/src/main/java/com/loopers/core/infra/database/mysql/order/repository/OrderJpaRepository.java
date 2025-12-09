package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.infra.database.mysql.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>, OrderQuerydslRepository {

    Optional<OrderEntity> findByOrderKey(String orderKey);
}
