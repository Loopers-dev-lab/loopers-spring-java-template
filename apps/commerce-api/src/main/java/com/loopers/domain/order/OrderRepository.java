package com.loopers.domain.order;

import com.loopers.domain.user.UserModel;
import java.util.Optional;
import java.util.List;

public interface OrderRepository {
    // 주문 저장
    OrderModel save(OrderModel order);
    // 주문 단건 조회
    Optional<OrderModel> findById(Long id);
    // 사용자 주문 조회
    List<OrderModel> findByUserId(UserModel user);
}

