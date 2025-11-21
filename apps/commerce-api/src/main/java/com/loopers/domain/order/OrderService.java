
package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
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
