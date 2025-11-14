package com.loopers.application.order;

import com.loopers.domain.order.CreateOrderService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserPointService;
import com.loopers.domain.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final UserService userService;
  private final PointService pointService;
  private final ProductService productService;
  private final OrderService orderService;
  private final CreateOrderService createOrderService;

  private final ProductStockService productStockService;
  private final UserPointService userPointService;

  public Page<Order> getOrderList(Long userId,
                                  String sortType,
                                  int page,
                                  int size) {
    return orderService.getOrders(userId, sortType, page, size);
  }

  public OrderInfo getOrderDetail(Long orderId) {
    Order order = orderService.getOrder(orderId);
    return OrderInfo.from(order);
  }

  @Transactional
  public OrderInfo createOrder(CreateOrderCommand command) {

    Map<Long, Long> quantityMap = command.orderItemInfo();
    User user = userService.getActiveUser(command.userId());
    List<Product> productList = productService.getExistingProducts(quantityMap.keySet());
    Order savedOrder = null;
    productStockService.deduct(productList, quantityMap);
    userPointService.use(user, productList, quantityMap);

    savedOrder = createOrderService.save(user, productList, quantityMap);
    return OrderInfo.from(savedOrder);
  }
}
