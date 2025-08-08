package com.loopers.domain.order.item;

import com.loopers.domain.order.OrderModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository {

    OrderItemModel save(OrderItemModel orderItemModel);
    
    void deleteAll();
    
    Optional<OrderItemModel> findById(Long orderId);
    
    List<OrderModel> findByUserId(Long userId);
    
    Page<OrderModel> findByUserId(Long userId, Pageable pageable);
    
    Page<OrderModel> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
}
