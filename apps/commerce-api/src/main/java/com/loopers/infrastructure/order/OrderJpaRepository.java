package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.product p " +
            "JOIN FETCH p.brand " +
            "WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<Order> findByIdAndUser(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findAllByUser(@Param("userId") Long userId);
}
