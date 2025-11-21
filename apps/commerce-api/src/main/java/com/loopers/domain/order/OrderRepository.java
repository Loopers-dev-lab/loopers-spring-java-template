package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository{
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Order save(Order order);

    Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
