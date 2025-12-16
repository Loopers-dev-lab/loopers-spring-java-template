package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Order save(Order order);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.issuedCoupon " +
            "WHERE o.id = :orderId")
    Optional<Order> findOrderWithDetailsById(@Param("orderId") Long orderId);
}
