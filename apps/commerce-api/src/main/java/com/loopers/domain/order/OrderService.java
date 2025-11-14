package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;

  public Optional<Order> getById(Long orderId) {
    return orderRepository.findById(orderId);
  }

  public Optional<Order> getWithItemsById(Long orderId) {
    return orderRepository.findWithItemsById(orderId);
  }

  public Order create(Long userId, List<OrderItem> orderItems, LocalDateTime orderedAt) {
    Long totalAmount = calculateTotalAmount(orderItems);

    Order order = Order.of(userId, OrderStatus.PAYMENT_FAILED, totalAmount, orderedAt);

    orderItems.forEach(order::addItem);

    return orderRepository.save(order);
  }

  public Order completeOrder(Long orderId) {
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.complete();
    return orderRepository.save(order);
  }

  public Order markOrderAsPaymentPending(Long orderId) {
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.failPayment();
    return orderRepository.save(order);
  }

  public Order retryCompleteOrder(Long orderId) {
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.retryComplete();
    return orderRepository.save(order);
  }

  private Long calculateTotalAmount(List<OrderItem> orderItems) {
    return orderItems.stream()
        .mapToLong(item -> item.getOrderPriceValue() * item.getQuantityValue())
        .sum();
  }
}
