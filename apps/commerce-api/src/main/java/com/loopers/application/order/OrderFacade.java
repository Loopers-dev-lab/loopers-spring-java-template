package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final UserService userService;
  private final ProductService productService;
  private final StockService stockService;
  private final OrderService orderService;
  private final PointService pointService;

  @Transactional(readOnly = true)
  public Page<Order> getOrderList(Long userId,
                                  String sortType,
                                  int page,
                                  int size) {
    return orderService.getOrders(userId, sortType, page, size);
  }

  @Transactional(readOnly = true)
  public OrderInfo getOrderDetail(Long orderId) {
    Order order = orderService.getOrder(orderId);
    return OrderInfo.from(order);
  }

  @Transactional
  public OrderInfo createOrder(CreateOrderCommand command) {

    List<CreateOrderCommand.OrderItemCommand> orderItemCommands = command.orderItemCommands();
    User user = userService.getActiveUser(command.userId());

    // 2. Command -> Domain 객체(OrderItem) 변환
    List<OrderItem> orderItems = orderItemCommands.stream()
        .map(cmd -> {
          Product product = productService.getProduct(cmd.productId());
          stockService.deduct(product.getId(), cmd.quantity()); // 재고 차감
          return OrderItem.create(product.getId(), cmd.quantity(), product.getPrice());
        }).toList();

    // 3. 주문 생성
    Order savedOrder = orderService.save(Order.create(user.getId(), orderItems));

    // 4. 포인트 차감(비관적락)
    pointService.use(user.getId(), savedOrder.getTotalPrice().getAmount());
    savedOrder.paid();
    return OrderInfo.from(savedOrder);
  }
}
