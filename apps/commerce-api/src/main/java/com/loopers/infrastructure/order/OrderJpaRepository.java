package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    List<Order> findAllByUserId(Long id);

    List<Order> findAllByStatusAndCreatedAtBefore(OrderStatus status, ZonedDateTime threshold);

    @Query("""
        select o from Order o
        join fetch o.items
        where o.id = :orderId
          and o.userId = :userId
    """)
    Optional<Order> findOrderWithItems(@Param("userId") Long userId, @Param("orderId") Long orderId);
}
