package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;

  public Optional<Order> getById(Long orderId) {
    return orderRepository.findById(orderId);
  }

  public Optional<Order> getWithItemsById(Long orderId) {
    Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");

    return orderRepository.findWithItemsById(orderId);
  }

  public Page<OrderListDto> findOrders(Long userId, Pageable pageable) {
    Objects.requireNonNull(userId, "사용자 ID는 null일 수 없습니다.");
    Objects.requireNonNull(pageable, "Pageable은 null일 수 없습니다.");
    return orderRepository.findOrderList(userId, pageable);
  }

  public Order create(Long userId, List<OrderItem> orderItems, LocalDateTime orderedAt) {
    Long totalAmount = calculateTotalAmount(orderItems);

    Order order = Order.of(userId, OrderStatus.PENDING, totalAmount, orderedAt);

    orderItems.forEach(order::addItem);

    return orderRepository.save(order);
  }

  public Order completeOrder(Long orderId) {
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.complete();
    return orderRepository.save(order);
  }

  public Order failPaymentOrder(Long orderId) {
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
