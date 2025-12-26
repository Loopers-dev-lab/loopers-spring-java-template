package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findPendingPaymentOrdersBefore(ZonedDateTime before);

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByIdAndUserIdForUpdate(Long orderId, Long userId);
}
