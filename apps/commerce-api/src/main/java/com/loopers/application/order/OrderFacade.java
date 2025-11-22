package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreateService;
import com.loopers.domain.order.OrderListDto;
import com.loopers.domain.order.OrderPreparation;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

  private final OrderService orderService;
  private final OrderCreateService orderCreateService;
  private final ProductService productService;
  private final PointService pointService;
  private final Clock clock;


  @Transactional
  public Order createOrder(Long userId, List<OrderItemCommand> commands) {
    LocalDateTime orderedAt = LocalDateTime.now(clock);

    Map<Long, Product> productById = getProductByIdWithLocks(commands);

    OrderPreparation result = orderCreateService.prepareOrder(commands, productById);

    pointService.deduct(userId, result.totalAmount());

    result.orderItems().decreaseStock(productById);

    return orderService.create(userId, result.orderItems().getItems(), orderedAt);
  }

  @Transactional(readOnly = true)
  public Page<OrderListDto> retrieveOrders(Long userId, Pageable pageable) {
    return orderService.findOrders(userId, pageable);
  }

  @Transactional(readOnly = true)
  public Order retrieveOrderDetail(Long userId, Long orderId) {
    Order order = orderService.getWithItemsById(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    if (!Objects.equals(order.getUserId(), userId)) {
      throw new CoreException(ErrorType.FORBIDDEN, "본인의 주문만 조회할 수 있습니다.");
    }

    return order;
  }

  private Map<Long, Product> getProductByIdWithLocks(List<OrderItemCommand> commands) {
    List<Long> productIds = commands.stream()
        .map(OrderItemCommand::productId)
        .distinct()
        .toList();

    return productService.findByIdsWithLock(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
  }
}
