
package com.loopers.domain.order;

import com.loopers.application.event.OrderCancelledEvent;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class OrderService {
  private final OrderRepository orderRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final OutboxService outboxService;

  public Page<Order> getOrders(
      Long userId,
      String sortType,
      int page,
      int size
  ) {
    Sort sort = this.getSortBySortType(sortType);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Order> orders = null;
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "유저 정보가 없습니다.");
    }
    orders = orderRepository.findByUserId(userId, pageable);
    return orders;
  }

  @Transactional(readOnly = true)
  public Order getOrder(Long id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return orderRepository.findById(id).orElse(null);
  }

  @Transactional
  public Order save(Order order) {
    return orderRepository.save(order);
  }

  @Transactional
  public void completePayment(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));

    order.paid();
    orderRepository.save(order);
  }

  @Transactional
  public void cancelPayment(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));

    order.cancel();
    orderRepository.save(order);

    // Outbox 패턴으로 주문 취소 이벤트 저장
    OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(
        orderId,
        order.getRefUserId(),
        "결제 취소"
    );
    
    outboxService.saveEvent(
        "Order", 
        orderId.toString(), 
        "OrderCancelled", 
        orderCancelledEvent
    );

    // 기존 동기 이벤트도 유지 (내부 처리용)
    eventPublisher.publishEvent(orderCancelledEvent);
  }

  private Sort getSortBySortType(String sortType) {
    if (sortType == null) sortType = "latest";
    Sort latestSort = Sort.by("createdAt").descending();
    switch (sortType.toLowerCase()) {
      case "latest":
        return latestSort;
      case "price":
        return Sort.by("status").descending();
      default:
        return latestSort;
    }
  }

}
