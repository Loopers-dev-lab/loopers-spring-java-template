package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

  private final OrderRepository orderRepository;

  public Order getById(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
  }

  public Order getWithItemsById(Long orderId) {
    return orderRepository.findWithItemsById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
  }

  @Transactional
  public Order create(Long userId, List<OrderItem> orderItems, LocalDateTime orderedAt) {
    Long totalAmount = calculateTotalAmount(orderItems);

    Order order = Order.of(userId, OrderStatus.PAYMENT_PENDING, totalAmount, orderedAt);

    orderItems.forEach(order::addItem);

    return orderRepository.save(order);
  }

  @Transactional
  public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
    Order order = getById(orderId);
    order.updateStatus(newStatus);
    return order;
  }

  private Long calculateTotalAmount(List<OrderItem> orderItems) {
    return orderItems.stream()
        .mapToLong(item -> item.getOrderPriceValue() * item.getQuantityValue())
        .sum();
  }
}
