package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreateService;
import com.loopers.domain.order.OrderPreparation;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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

  @Transactional
  public Order createOrder(Long userId, List<OrderItemCommand> commands, LocalDateTime orderedAt) {
    Objects.requireNonNull(userId, "유저아이디는 null 일수 없습니다.");
    Objects.requireNonNull(commands, "상품 주문 정보는 null일 수 없습니다.");
    Objects.requireNonNull(orderedAt, "주문 요청 시간은 null일 수 없습니다");

    Map<Long, Product> productById = getProductMapWithLock(commands);

    OrderPreparation result = orderCreateService.prepareOrder(commands, productById);

    pointService.deduct(userId, result.totalAmount());

    result.orderItems().decreaseStock(productById);

    return orderService.create(userId, result.orderItems().getItems(), orderedAt);
  }

  private Map<Long, Product> getProductMapWithLock(List<OrderItemCommand> commands) {
    List<Long> productIds = commands.stream()
        .map(OrderItemCommand::productId)
        .distinct()
        .toList();

    return productService.findByIdsWithLock(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
  }
}
