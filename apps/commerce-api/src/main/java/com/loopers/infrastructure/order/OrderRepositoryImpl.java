package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OrderRepository 구현체
 * JPA를 사용한 영속성 관리
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity;

        if (order.getId() == null) {
            // 신규 저장
            entity = OrderEntity.from(order);
            entity = jpaRepository.save(entity);
        } else {
            // 업데이트
            entity = jpaRepository.findById(order.getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + order.getId()));
            entity.updateFrom(order);
            entity = jpaRepository.save(entity);
        }

        return entity.toDomain();
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return jpaRepository.findById(orderId)
                .map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndStatus(String userId, OrderStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long orderId) {
        return jpaRepository.existsById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll().stream()
                .map(OrderEntity::toDomain)
                .collect(Collectors.toList());
    }
}
