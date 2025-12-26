package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, ZonedDateTime createdAt);

    Optional<Order> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.userId = :userId AND o.deletedAt IS NULL")
    Optional<Order> findByIdAndUserIdForUpdate(Long orderId, Long userId);
}
