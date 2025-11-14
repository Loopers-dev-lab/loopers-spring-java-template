package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.orderitem.OrderItems;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Products;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {

  private final OrderService orderService;
  private final ProductService productService;
  private final PointService pointService;
  private final OrderItemFactory orderItemFactory;

  @Transactional
  public Order createOrder(Long userId, List<OrderItemCommand> commands, LocalDateTime orderedAt) {
    List<Long> productIds = commands.stream()
        .map(OrderItemCommand::productId)
        .toList();

    Products products = Products.from(productService.getByIdsWithLock(productIds));

    List<OrderItem> items = commands.stream()
        .map(cmd -> orderItemFactory.create(cmd, products))
        .toList();

    OrderItems orderItems = new OrderItems(items);

    orderItems.validateStock(products);

    Long totalAmount = orderItems.calculateTotalAmount();

    pointService.deduct(userId, totalAmount);

    orderItems.decreaseStock(products);

    return orderService.create(userId, orderItems.getItems(), orderedAt);
  }
}
