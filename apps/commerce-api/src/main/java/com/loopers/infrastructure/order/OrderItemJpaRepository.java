package com.loopers.infrastructure.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.order.OrderItemEntity;

/**
 * 주문 항목 JPA Repository
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    List<OrderItemEntity> findByOrderId(Long orderId);

    /**
     * 주문 ID로 주문 항목 개수를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 개수
     */
    int countByOrderId(Long orderId);

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 항목 목록
     */
    org.springframework.data.domain.Page<OrderItemEntity> findByOrderId(
            Long orderId,
            org.springframework.data.domain.Pageable pageable);
}
