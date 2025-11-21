package com.loopers.domain.order;

import java.util.List;

/**
 * 주문 항목 Repository 인터페이스
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderItemRepository {

    /**
     * 주문 항목을 저장합니다.
     *
     * @param orderItem 저장할 주문 항목 엔티티
     * @return 저장된 주문 항목 엔티티
     */
    OrderItemEntity save(OrderItemEntity orderItem);

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    List<OrderItemEntity> findByOrderId(Long orderId);

    void saveAll(List<OrderItemEntity> orderItemsByOrderId);

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
