package com.loopers.infrastructure.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderStatus;

/**
 * 주문 JPA Repository
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 주문 ID로 삭제되지 않은 주문을 조회합니다.
     *
     * @param id 주문 ID
     * @return 주문 엔티티 (Optional)
     */
    java.util.Optional<OrderEntity> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 사용자 ID로 삭제되지 않은 주문 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    List<OrderEntity> findByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 사용자 ID로 삭제되지 않은 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    Page<OrderEntity> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * 사용자 ID와 주문 상태로 삭제되지 않은 주문 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    Page<OrderEntity> findByUserIdAndStatusAndDeletedAtIsNull(Long userId, OrderStatus status, Pageable pageable);
}
