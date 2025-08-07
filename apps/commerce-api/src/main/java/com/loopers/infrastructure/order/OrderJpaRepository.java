package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.embeded.OrderUserId;
import com.loopers.domain.order.embeded.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderModel, Long> {
    
    List<OrderModel> findByUserIdOrderByCreatedAtDesc(OrderUserId userId);
    
    Page<OrderModel> findByUserIdOrderByCreatedAtDesc(OrderUserId userId, Pageable pageable);
    
    Page<OrderModel> findByUserIdAndStatusOrderByCreatedAtDesc(OrderUserId userId, OrderStatus status, Pageable pageable);
}
