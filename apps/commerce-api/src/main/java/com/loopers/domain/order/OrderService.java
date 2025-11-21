package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderService {

  private final OrderRepository orderRepository;

  public Order createOrder(Long userId, List<OrderItem> orderItems) {
    return orderRepository.save(new Order(userId, orderItems));
  }

  public List<Order> getOrders(Long userId) {
    List<Order> orders = orderRepository.findByUserId(userId);

    return orders;
  }

  public Order getOrder(Long id) {
    return orderRepository.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
  }
}
