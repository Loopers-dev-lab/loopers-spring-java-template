package com.loopers.infrastructure.order;

import com.loopers.domain.order.OpderRepository;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.embeded.OrderUserId;
import com.loopers.domain.order.embeded.OrderStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepositortyImpl implements OpderRepository {
    private final OrderJpaRepository orderJpaRepository;
    public final JPAQueryFactory queryFactory;

    public OrderRepositortyImpl(OrderJpaRepository orderJpaRepository, JPAQueryFactory queryFactory) {
        this.orderJpaRepository = orderJpaRepository;
        this.queryFactory = queryFactory;
    }

    @Override
    public OrderModel save(OrderModel orderModel) {
        return orderJpaRepository.save(orderModel);
    }
    @Override
    public void deleteAll() {
        orderJpaRepository.deleteAll();
    }

    @Override
    public Optional<OrderModel> findById(Long orderId) {
        return orderJpaRepository.findById(orderId);
    }

    @Override
    public List<OrderModel> findByUserId(Long userId) {
        return orderJpaRepository.findByUserIdOrderByCreatedAtDesc(OrderUserId.of(userId));
    }

    @Override
    public Page<OrderModel> findByUserId(Long userId, Pageable pageable) {
        return orderJpaRepository.findByUserIdOrderByCreatedAtDesc(OrderUserId.of(userId), pageable);
    }

    @Override
    public Page<OrderModel> findByUserIdAndStatus(Long userId, String status, Pageable pageable) {
        return orderJpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(OrderUserId.of(userId), OrderStatus.of(status), pageable);
    }
}
