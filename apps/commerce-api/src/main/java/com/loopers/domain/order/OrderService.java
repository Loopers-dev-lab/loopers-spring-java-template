
package com.loopers.domain.order;

import com.loopers.application.event.OrderCancelledEvent;
import com.loopers.application.event.OrderPaidEvent;
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
  public Order getOrder(String id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return orderRepository.findByOrderId(id).orElse(null);
  }

  @Transactional(readOnly = true)
  public Order getOrderByOrderId(String orderId) {
    if (orderId == null || orderId.isEmpty()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "주문ID가 없습니다.");
    }
    return orderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));
  }

  @Transactional
  public Order save(Order order) {
    return orderRepository.save(order);
  }

  @Transactional
  public void completePayment(String orderId) {
    Order order = orderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));

    order.paid();
    orderRepository.save(order);

    // 주문 결제 완료 이벤트 발행 (랭킹 집계용)
    OrderPaidEvent orderPaidEvent = new OrderPaidEvent(
        order.getOrderId(),
        order.getRefUserId(),
        order.getTotalPrice()
    );

    eventPublisher.publishEvent(orderPaidEvent);
  }

  @Transactional
  public void cancelPayment(String orderId) {
    Order order = orderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId));

    order.cancel();
    orderRepository.save(order);

    // 주문 취소 이벤트 발행 (배치 처리용)
    OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(
        orderId,
        order.getRefUserId(),
        "결제 취소"
    );

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
