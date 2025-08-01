package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OpderRepository {

    OrderModel save(OrderModel orderModel);
    
    void deleteAll();
    
    Optional<OrderModel> findById(Long orderId);
    
    List<OrderModel> findByUserId(Long userId);
    
    Page<OrderModel> findByUserId(Long userId, Pageable pageable);
    
    Page<OrderModel> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
}
