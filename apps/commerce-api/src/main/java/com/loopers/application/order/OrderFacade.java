package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final UserService userService;
  private final UserRepository userRepository;
  private final ProductService productService;
  private final StockService stockService;
  private final OrderService orderService;
  private final PointService pointService;
  private final PointRepository pointRepository;

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
    Long userId = command.userId();
    List<Long> productIds = orderItemCommands.stream().map(CreateOrderCommand.OrderItemCommand::productId).toList();
    List<Product> products = productService.getExistingProducts(productIds);

    // 2. Command -> Domain 객체(OrderItem) 변환
    for (CreateOrderCommand.OrderItemCommand item : orderItemCommands) {
      stockService.deduct(item.productId(), item.quantity()); // 재고 차감
    }
    // 3. 주문 생성
    List<OrderItem> orderItems = orderItemCommands.stream()
        .map(item ->
        {
          Long productId = item.productId();
          Product product = products.stream().filter(p -> p.getId().equals(productId)).findFirst()
              .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ""));
          return OrderItem.create(productId, item.quantity(), product.getPrice());
        })
        .toList();
    Order order = Order.create(userId, orderItems);
    Order savedOrder = orderService.save(order);
    savedOrder.paid();

    // 4. 포인트 차감(비관적락)
    pointService.use(userId, order.getTotalPrice().getAmount());
    return OrderInfo.from(savedOrder);
  }

}
