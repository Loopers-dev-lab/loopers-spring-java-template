package com.loopers.domain.order;

import com.loopers.domain.common.event.DomainEventPublisher;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final DomainEventPublisher eventPublisher;

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

  public Order create(OrderCreateCommand command) {
    Objects.requireNonNull(command, "command는 null일 수 없습니다.");

    Order order = Order.of(
        command.userId(),
        OrderStatus.PENDING,
        command.totalAmount(),
        command.pointUsedAmount(),
        command.pgAmount(),
        command.couponId(),
        command.discountAmount(),
        command.orderedAt()
    );

    command.orderItems().forEach(order::addItem);

    Order savedOrder = orderRepository.save(order);
    savedOrder.registerCreatedEvent(command);
    savedOrder.publishEvents(eventPublisher);

    return savedOrder;
  }

  public Order completeOrder(Long orderId, LocalDateTime completedAt) {
    Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");
    Objects.requireNonNull(completedAt, "completedAt은 null일 수 없습니다.");
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.complete(completedAt);
    Order savedOrder = orderRepository.save(order);
    order.publishEvents(eventPublisher);
    return savedOrder;
  }

  public void failPaymentOrder(Long orderId) {
    Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.failPayment();
    orderRepository.save(order);
  }

  public Order retryCompleteOrder(Long orderId) {
    Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");
    Order order = getById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    order.retryComplete();
    return orderRepository.save(order);
  }
}
