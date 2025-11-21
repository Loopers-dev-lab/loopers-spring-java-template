package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 주문 Repository 인터페이스
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderRepository {

    /**
     * 주문을 저장합니다.
     *
     * @param order 저장할 주문 엔티티
     * @return 저장된 주문 엔티티
     */
    OrderEntity save(OrderEntity order);

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param id 주문 ID
     * @return 조회된 주문 엔티티 (Optional)
     */
    Optional<OrderEntity> findById(Long id);

    /**
     * 사용자 ID로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    Page<OrderEntity> findByUserId(
            Long userId,
            Pageable pageable);

    /**
     * 사용자 ID와 주문 상태로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param status   주문 상태
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    Page<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
}
