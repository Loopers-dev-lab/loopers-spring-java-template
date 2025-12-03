package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA Repository
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 사용자 ID로 주문 조회
     */
    List<OrderEntity> findByUserId(String userId);

    /**
     * 주문 상태로 조회
     */
    List<OrderEntity> findByStatus(OrderStatus status);

    /**
     * 사용자 ID와 주문 상태로 조회
     */
    List<OrderEntity> findByUserIdAndStatus(String userId, OrderStatus status);

    /**
     * 특정 상품이 포함된 주문 조회
     */
    @Query("SELECT DISTINCT o FROM OrderEntity o JOIN o.orderItems oi WHERE oi.productId = :productId")
    List<OrderEntity> findByProductId(@Param("productId") Long productId);
}
