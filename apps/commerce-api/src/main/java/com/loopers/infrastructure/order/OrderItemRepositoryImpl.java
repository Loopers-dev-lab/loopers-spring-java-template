package com.loopers.infrastructure.order;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 항목 Repository 구현체
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public OrderItemEntity save(OrderItemEntity orderItem) {
        return orderItemJpaRepository.save(orderItem);
    }

    @Override
    public List<OrderItemEntity> findByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }

    @Override
    public void saveAll(List<OrderItemEntity> orderItemsByOrderId) {
        orderItemJpaRepository.saveAll(orderItemsByOrderId);
    }

    @Override
    public int countByOrderId(Long orderId) {
        return orderItemJpaRepository.countByOrderId(orderId);
    }

    @Override
    public org.springframework.data.domain.Page<OrderItemEntity> findByOrderId(
            Long orderId,
            org.springframework.data.domain.Pageable pageable) {
        return orderItemJpaRepository.findByOrderId(orderId, pageable);
    }
}
