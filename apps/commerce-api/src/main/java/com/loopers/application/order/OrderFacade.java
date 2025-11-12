package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.orderitem.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Products;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderFacade (Application Layer)
 * 주문 생성 시 여러 도메인 서비스를 조율
 * - 상품 재고 검증 및 차감
 * - 포인트 잔액 검증 및 차감
 * - 주문 생성
 */
@Component
@RequiredArgsConstructor
public class OrderFacade {

  private final OrderService orderService;
  private final ProductService productService;
  private final PointService pointService;

  @Transactional
  public Order createOrder(Long userId, List<OrderItemCommand> commands, LocalDateTime orderedAt) {
    List<OrderItem> requestedOrderItems = commands.stream()
        .map(cmd -> OrderItem.of(
            cmd.productId(),
            cmd.productName(),
            cmd.quantity(),
            cmd.orderPrice()
        ))
        .toList();

    OrderItems orderItems = OrderItems.from(requestedOrderItems);

    Products products = Products.from(productService.getByIdsWithLock(orderItems.getProductIds()));

    orderItems.validateStock(products);

    Long totalAmount = orderItems.calculateTotalAmount();

    pointService.checkBalance(userId, totalAmount);

    orderItems.decreaseStock(products);

    pointService.deduct(userId, totalAmount);

    return orderService.create(userId, orderItems.getItems(), orderedAt);
  }
}
